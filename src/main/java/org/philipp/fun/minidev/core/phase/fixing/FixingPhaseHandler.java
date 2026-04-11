package org.philipp.fun.minidev.core.phase.fixing;

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

@Component
public class FixingPhaseHandler implements PhaseHandler {

    private static final Logger log = LoggerFactory.getLogger(FixingPhaseHandler.class);

    private final NotificationSseService notificationSseService;
    private final TerminalSseService terminalSseService;
    private final LlmClient llmClient;

    public FixingPhaseHandler(
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

        log.info("Fixing phase for run {}", run.getRunId());
        run.incrementFixingIterations();
        terminalSseService.sendTerminalText("Fixing identified issues and implementing missing To-Dos...\n", SseEventType.AGENT_WORK, 50);

        String currentCode = "";
        try {
            if (Files.exists(metadata.htmlPath())) {
                currentCode = Files.readString(metadata.htmlPath());
            }
        } catch (IOException e) {
            log.error("Failed to read code for fixing from {}", metadata.htmlPath(), e);
        }

        String todosFormatted = String.join("\n", metadata.todos().stream().map(t -> "- " + t).toList());

        LlmRequest request = new LlmRequest(
                List.of(
                        LlmRequest.Message.system(String.format("""
                                You are a professional web developer. Your task is to fix issues and implement missing features in a game called '%s'.
                                The game concept is: %s
                                
                                Current To-Do List of remaining tasks:
                                %s
                                
                                Current Implementation (HTML/JS/CSS):
                                --- START CODE ---
                                %s
                                --- END CODE ---
                                
                                Instructions:
                                1. Review the current implementation and the To-Do list.
                                2. Update the code to address the remaining To-Dos.
                                3. Fix any bugs or inconsistencies you find.
                                4. Ensure the game remains a single HTML file with embedded CSS and JS.
                                
                                Respond ONLY with the complete, updated raw code content. No markdown code blocks, no explanations.
                                """, metadata.name(), metadata.concept(), todosFormatted, currentCode)),
                        LlmRequest.Message.user("Please provide the updated code.")
                )
        );

        LlmResponse response = llmClient.chat(request);

        if (response.success()) {
            String updatedCode = response.content().trim();
            try {
                Files.createDirectories(metadata.htmlPath().getParent());
                Files.writeString(metadata.htmlPath(), updatedCode);
                log.info("Saved fixed code to {}", metadata.htmlPath());
                terminalSseService.sendTerminalText("Issues fixed and code updated.\n", SseEventType.AGENT_WORK, 50);
            } catch (IOException e) {
                log.error("Failed to save fixed code to {}", metadata.htmlPath(), e);
            }
        } else {
            log.error("Failed to generate fixed code: {}", response.errorMessage());
            notificationSseService.sendNotification("Fixing failed: " + response.errorMessage());
        }
    }
}
