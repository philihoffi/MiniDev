package org.philipp.fun.minidev.core.phase.testing;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.philipp.fun.minidev.core.phase.PhaseHandler;
import org.philipp.fun.minidev.run.AgentRun;
import org.philipp.fun.minidev.run.GameMetadata;
import org.philipp.fun.minidev.web.service.TerminalSseService;
import org.philipp.fun.minidev.web.service.AbstractSseService.SseEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class TestingPhaseHandler implements PhaseHandler {

    private static final Logger log = LoggerFactory.getLogger(TestingPhaseHandler.class);
    private final TerminalSseService terminalSseService;

    public TestingPhaseHandler(TerminalSseService terminalSseService) {
        this.terminalSseService = terminalSseService;
    }

    @Override
    public void execute(AgentRun run) {
        GameMetadata metadata = run.getGameMetadata();
        if (metadata == null || metadata.htmlPath() == null) {
            log.error("No metadata or HTML path found for run {}", run.getGameMetadata().runId());
            terminalSseService.sendTerminalText("No HTML file found to test.", SseEventType.AGENT_WORK, 50);
            return;
        }

        UUID runId = metadata.runId();
        Path htmlPath = metadata.htmlPath();
        log.info("Starting testing phase for run {} on file {}", runId, htmlPath);
        terminalSseService.sendTerminalText("Starting static code analysis for " + htmlPath.getFileName() + "...", SseEventType.AGENT_WORK, 50);

        AnalysisResult result = performStaticAnalysis(htmlPath, runId);

        if (result.findings().isEmpty()) {
            terminalSseService.sendTerminalText("Static analysis passed! No issues found.", SseEventType.AGENT_WORK, 50);
        } else {
            terminalSseService.sendTerminalText("Static analysis failed with " + result.findings().size() + " findings:", SseEventType.AGENT_WORK, 50);
            for (String finding : result.findings()) {
                terminalSseService.sendTerminalText("- " + finding, SseEventType.AGENT_WORK, 50);
            }
        }

        // Always try to format, even if there are findings (unless there were fatal errors reading the file)
        if (result.document() != null) {
            terminalSseService.sendTerminalText("Running linter/formatter...", SseEventType.AGENT_WORK, 50);
            formatAndSave(htmlPath, result.document());
            terminalSseService.sendTerminalText("Formatting complete.", SseEventType.AGENT_WORK, 50);
        }

        if (!result.findings().isEmpty()) {
            run.getGameMetadata().todos().addAll(result.findings());
        }
    }

    private record AnalysisResult(List<String> findings, Document document) {}

    private AnalysisResult performStaticAnalysis(Path htmlPath, UUID runId) {
        List<String> findings = new ArrayList<>();

        if (!Files.exists(htmlPath)) {
            log.warn("HTML file does not exist for run {}: {}", runId, htmlPath);
            findings.add("File index.html does not exist.");
            return new AnalysisResult(findings, null);
        }

        try {
            String content = Files.readString(htmlPath);
            if (content.isBlank()) {
                log.warn("HTML file is empty for run {}: {}", runId, htmlPath);
                findings.add("index.html is empty.");
                return new AnalysisResult(findings, null);
            }

            Document doc = Jsoup.parse(content);
            log.debug("Performing static analysis on run {} ({} characters)", runId, content.length());

            // 1. Basic structure checks
            checkBasicStructure(doc, findings);

            // 2. Resource checks
            checkResources(doc, findings);

            // 3. Accessibility & Best Practices
            checkBestPractices(doc, findings);

            // 4. Script checks (basic)
            checkScripts(doc, findings);

            log.info("Static analysis for run {} finished with {} findings", runId, findings.size());
            return new AnalysisResult(findings, doc);

        } catch (IOException e) {
            log.error("Failed to read HTML file for analysis on run {}", runId, e);
            findings.add("Error reading index.html: " + e.getMessage());
            return new AnalysisResult(findings, null);
        } catch (Exception e) {
            log.error("Unexpected error during static analysis on run {}", runId, e);
            findings.add("Unexpected error during analysis: " + e.getMessage());
            return new AnalysisResult(findings, null);
        }
    }

    private void checkBasicStructure(Document doc, List<String> findings) {
        if (doc.select("head").isEmpty()) findings.add("Missing <head> tag.");
        if (doc.select("body").isEmpty()) findings.add("Missing <body> tag.");
        if (doc.title().isBlank()) findings.add("Empty or missing <title> tag.");
        if (doc.select("html").attr("lang").isBlank()) findings.add("Missing 'lang' attribute in <html> tag.");
    }

    private void checkResources(Document doc, List<String> findings) {
        // Check for missing alt attributes on images
        doc.select("img").forEach(img -> {
            if (!img.hasAttr("alt")) {
                findings.add("Image missing 'alt' attribute: " + img.outerHtml());
            }
        });

        // Check for broken internal anchor links (simplified)
        doc.select("a[href^=#]").forEach(a -> {
            String targetId = a.attr("href").substring(1);
            if (!targetId.isEmpty() && doc.getElementById(targetId) == null) {
                findings.add("Broken internal link: #" + targetId);
            }
        });
    }

    private void checkBestPractices(Document doc, List<String> findings) {
        // Check for viewport meta tag
        if (doc.select("meta[name=viewport]").isEmpty()) {
            findings.add("Missing responsive viewport meta tag.");
        }

        // Check for duplicate IDs
        Elements elementsWithId = doc.select("[id]");
        List<String> ids = new ArrayList<>();
        elementsWithId.forEach(el -> {
            String id = el.id();
            if (ids.contains(id)) {
                findings.add("Duplicate ID found: " + id);
            }
            ids.add(id);
        });
    }

    private void checkScripts(Document doc, List<String> findings) {
        Elements scripts = doc.select("script");
        if (scripts.isEmpty()) {
            findings.add("No <script> tags found. Game might be missing logic.");
        }

        scripts.forEach(script -> {
            String scriptContent = script.data();
            if (scriptContent.contains("console.error")) {
                // This might be okay, but could be leftover debugging
            }
            // Basic check for common syntax errors (very primitive)
            if (scriptContent.contains("function") && !scriptContent.contains("{")) {
                 findings.add("Potential syntax error in script: function keyword without block.");
            }
        });
    }

    private void formatAndSave(Path path, Document doc) {
        try {
            doc.outputSettings()
                    .indentAmount(4)
                    .prettyPrint(true)
                    .outline(true);

            String formattedHtml = doc.outerHtml();
            Files.writeString(path, formattedHtml);
        } catch (IOException e) {
            log.error("Failed to save formatted HTML", e);
        }
    }
}
