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
            log.error("No metadata found for run {}", run.getGameMetadata().runId());
            run.transitionTo(RunState.FAILED);
            return;
        }

        log.info("Starting fixing phase for run {} (Iteration: {})", metadata.runId(), run.getFixingIterations() + 1);
        run.incrementFixingIterations();
        terminalSseService.sendTerminalText("Fixing identified issues and implementing missing To-Dos...\n", SseEventType.AGENT_WORK, 50);

        while (!run.getGameMetadata().todos().isEmpty()) {
            metadata = run.getGameMetadata();
            String nextTodo = metadata.todos().get(0);
            log.info("Fixing next To-Do for run {}: {}", metadata.runId(), nextTodo);
            terminalSseService.sendTerminalText("Implementing To-Do: " + nextTodo + "\n", SseEventType.AGENT_WORK, 50);

            String currentCode = "";
            try {
                if (Files.exists(metadata.htmlPath())) {
                    currentCode = Files.readString(metadata.htmlPath());
                }
            } catch (IOException e) {
                log.error("Failed to read code for fixing from {}", metadata.htmlPath(), e);
                break;
            }

            String doneTodosFormatted = String.join("\n", metadata.doneTodos().stream().map(t -> "- " + t).toList());

            LlmRequest request = new LlmRequest(
                    List.of(
                            LlmRequest.Message.system(String.format("""
                                    You are a professional web developer. Your task is to implement a specific feature in a game called '%s'.
                                    The game concept is: %s
                                    
                                    Already completed tasks:
                                    %s
                                    
                                    YOUR CURRENT TASK TO IMPLEMENT:
                                    %s
                                    
                                    Current Implementation (HTML/JS/CSS):
                                    --- START CODE ---
                                    %s
                                    --- END CODE ---
                                    
                                    Instructions:
                                    1. Review the current implementation and the completed tasks.
                                    2. Update the code to implement ONLY the 'CURRENT TASK TO IMPLEMENT'.
                                    3. Fix any bugs or inconsistencies related to this task.
                                    4. Ensure the game remains a single HTML file with embedded CSS and JS.
                                    
                                    Respond ONLY with the complete, updated raw code content. No markdown code blocks, no explanations.
                                    """, metadata.name(), metadata.concept(), doneTodosFormatted, nextTodo, currentCode)),
                            LlmRequest.Message.user("Please provide the updated code.")
                    )
            );

            LlmResponse response = llmClient.chat(request);

            if (response.success()) {
                String updatedCode = response.content().trim();
                try {
                    Files.createDirectories(metadata.htmlPath().getParent());
                    Files.writeString(metadata.htmlPath(), updatedCode);


                    run.getGameMetadata().doneTodos().add(run.getGameMetadata().todos().removeFirst());

                    log.info("Successfully saved {} characters of fixed code for run {} to {}", updatedCode.length(), metadata.runId(), metadata.htmlPath());
                    terminalSseService.sendTerminalText("To-Do '" + nextTodo + "' implemented.\n", SseEventType.AGENT_WORK, 50);
                } catch (IOException e) {
                    log.error("Failed to save fixed code for run {} to {}", metadata.runId(), metadata.htmlPath(), e);
                    break;
                }
            } else {
                log.error("LLM fixing failed for run {}: {}", metadata.runId(), response.errorMessage());
                notificationSseService.sendNotification("Fixing failed for To-Do '" + nextTodo + "': " + response.errorMessage());
                break;
            }
        }

        if (run.getGameMetadata().todos().isEmpty()) {
            log.info("All To-Dos completed for run {}", run.getGameMetadata().runId());
            terminalSseService.sendTerminalText("All To-Dos are completed.\n", SseEventType.AGENT_WORK, 50);
        } else {
            log.warn("Fixing phase ended with remaining To-Dos for run {}", run.getGameMetadata().runId());
        }
    }
}
