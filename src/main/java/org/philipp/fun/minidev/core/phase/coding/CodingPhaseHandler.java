package org.philipp.fun.minidev.core.phase.coding;

import org.philipp.fun.minidev.core.phase.PhaseHandler;
import org.philipp.fun.minidev.llm.LlmClient;
import org.philipp.fun.minidev.llm.LlmRequest;
import org.philipp.fun.minidev.llm.LlmResponse;
import org.philipp.fun.minidev.run.AgentRun;
import org.philipp.fun.minidev.run.AgentRun.RunState;
import org.philipp.fun.minidev.run.GameMetadata;
import org.philipp.fun.minidev.web.service.NotificationSseService;
import org.philipp.fun.minidev.web.service.TerminalSseService;
import org.philipp.fun.minidev.web.service.AbstractSseService.SseEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

@Component
public class CodingPhaseHandler implements PhaseHandler {

    private static final Logger log = LoggerFactory.getLogger(CodingPhaseHandler.class);

    private final NotificationSseService notificationSseService;
    private final TerminalSseService terminalSseService;
    private final LlmClient llmClient;

    public CodingPhaseHandler(
            NotificationSseService notificationSseService,
            TerminalSseService terminalSseService,
            LlmClient llmClient) {
        this.notificationSseService = notificationSseService;
        this.terminalSseService = terminalSseService;
        this.llmClient = llmClient;
    }

    @Override
    public void execute(AgentRun run) {
        GameMetadata metadata = run.getGameMetadata();
        if (metadata == null) {
            log.error("No metadata found for run {}", run.getRunId());
            run.transitionTo(RunState.FAILED);
            return;
        }

        log.info("Coding phase for run {}", run.getRunId());

        String todosFormatted = String.join("\n", metadata.todos().stream().map(t -> "- " + t).toList());
        Map<String, String> contextFiles = new java.util.HashMap<>();

        terminalSseService.sendTerminalText("Writing HTML...\n", SseEventType.AGENT_WORK, 50);
        StringBuilder contextBuilder = new StringBuilder();
        if (!contextFiles.isEmpty()) {
            contextBuilder.append("\nReference existing files for consistency:\n");
            contextFiles.forEach((name, content) -> {
                contextBuilder.append("--- FILE: ").append(name).append(" ---\n");
                contextBuilder.append(content).append("\n");
            });
        }

        LlmRequest request = new LlmRequest(
                List.of(
                        LlmRequest.Message.system(String.format("""
                                You are a professional web developer building a game called '%s' The game should be built with HTML, CSS, and vanilla JavaScript. JS and CSS should be embedded in the HTML.
                                The game concept is: %s
                                
                                To-Do List for implementation:
                                %s
                                
                                Instructions: %s
                                %s
                                
                                Respond ONLY with the raw code content. No markdown code blocks, no explanations.
                                """, metadata.name(), metadata.concept(), todosFormatted, "Write the HTML content for the game index page. Only return the code block with the HTML.", contextBuilder.toString())),
                        LlmRequest.Message.user("Please generate the code.")
                )
        );
        LlmResponse response = llmClient.chat(request);

        String code;
        if (response.success()) {
            code = response.content().trim();
        } else {
            log.error("Failed to generate code {}", response.errorMessage());
            run.transitionTo(RunState.FAILED);
            notificationSseService.sendNotification("Failed to generate code: " + response.errorMessage());
            return;
        }
        
        try {
            Files.createDirectories(metadata.htmlPath().getParent());
            Files.writeString(metadata.htmlPath(), code);
            log.info("Saved file to {}", metadata.htmlPath());
        } catch (IOException e) {
            log.error("Failed to save file to {}", metadata.htmlPath(), e);
        }
    }
}
