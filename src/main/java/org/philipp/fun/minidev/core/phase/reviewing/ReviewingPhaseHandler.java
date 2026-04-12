package org.philipp.fun.minidev.core.phase.reviewing;

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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ReviewingPhaseHandler implements PhaseHandler {

    private static final Logger log = LoggerFactory.getLogger(ReviewingPhaseHandler.class);

    private final NotificationSseService notificationSseService;
    private final TerminalSseService terminalSseService;
    private final LlmClient llmClient;

    public ReviewingPhaseHandler(
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

        log.info("Starting review phase for run {}", run.getGameMetadata().runId());
        terminalSseService.sendTerminalText("Reviewing code and updating To-Dos...\n", SseEventType.AGENT_WORK, 50);

        String code = "";
        try {
            if (Files.exists(metadata.htmlPath())) {
                code = Files.readString(metadata.htmlPath());
            }
        } catch (IOException e) {
            log.error("Failed to read code for review from {}", metadata.htmlPath(), e);
        }

        String todosFormatted = String.join("\n", metadata.todos().stream().map(t -> "- " + t).toList());

        LlmRequest request = new LlmRequest(
                List.of(
                        LlmRequest.Message.system(String.format("""
                                You are a professional code reviewer. You are reviewing the progress of a game called '%s'.
                                The game concept is: %s
                                
                                Current To-Do List:
                                %s
                                
                                Current Implementation (HTML/JS/CSS):
                                --- START CODE ---
                                %s
                                --- END CODE ---
                                
                                Task:
                                1. Check which To-Dos from the list have already been implemented in the code.
                                2. Create an updated To-Do List.
                                3. Remove To-Dos that are fully completed.
                                4. Keep To-Dos that are not yet or only partially implemented.
                                5. Do not add brand-new To-Dos. Only keep or remove items from the current list.
                                
                                Respond ONLY with the updated To-Do list as a bulleted list (using '-'). No other text.
                                """, metadata.name(), metadata.concept(), todosFormatted, code)),
                        LlmRequest.Message.user("Please provide the updated To-Do list.")
                )
        );

        LlmResponse response = llmClient.chat(request);

        if (response.success()) {
            run.getGameMetadata().todos().clear();
            List<String> updatedTodos = extractTodos(response.content().trim(), List.of());
            run.getGameMetadata().todos().addAll(updatedTodos);
            log.info("Successfully updated To-Dos for run {}. New count: {}", metadata.runId(), updatedTodos.size());
            log.debug("New To-Dos for run {}: {}", metadata.runId(), updatedTodos);
            terminalSseService.sendTerminalText("To-Do list updated based on review.", SseEventType.AGENT_WORK, 50);
        } else {
            log.error("LLM review failed for run {}: {}", metadata.runId(), response.errorMessage());
            notificationSseService.sendNotification("Review failed: " + response.errorMessage());
        }
    }

    private List<String> extractTodos(String text, List<String> fallbackTodos) {
        List<String> todos = new ArrayList<>();
        Pattern pattern = Pattern.compile("^\\s*-\\s*(.+)$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            todos.add(matcher.group(1).trim());
        }

        if (todos.isEmpty()) {
            return new ArrayList<>(fallbackTodos);
        }

        return todos;
    }
}
