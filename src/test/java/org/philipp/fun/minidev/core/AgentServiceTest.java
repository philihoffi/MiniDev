package org.philipp.fun.minidev.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.philipp.fun.minidev.llm.LlmClient;
import org.philipp.fun.minidev.llm.LlmResponse;
import org.philipp.fun.minidev.run.AgentRun;
import org.philipp.fun.minidev.run.GameMetadata;
import org.philipp.fun.minidev.web.service.AbstractSseService.SseEventType;
import org.philipp.fun.minidev.web.service.NotificationSseService;
import org.philipp.fun.minidev.web.service.TerminalSseService;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void reviewingKeepsTodoListEmptyWhenReviewerReturnsNoItems() throws Exception {
        AgentRun run = createRun(tempDir.resolve("review-run"), "Polish controls");
        AgentService service = createService(request -> LlmResponse.success("", "test-model", 0));

        invokePrivate(service, "performReviewing", run);

        assertTrue(run.getGameMetadata().todos().isEmpty());
    }

    @Test
    void fixingIncrementsFixingIterations() throws Exception {
        AgentRun run = createRun(tempDir.resolve("fix-run"), "Add pause menu");
        AgentService service = createService(request -> LlmResponse.success("<html><body>updated</body></html>", "test-model", 0));

        invokePrivate(service, "performFixing", run);

        assertEquals(1, run.getFixingIterations());
        assertTrue(Files.exists(run.getGameMetadata().htmlPath()));
    }

    private AgentRun createRun(Path runDirectory, String todo) {
        AgentRun run = new AgentRun();
        GameMetadata metadata = new GameMetadata(
                "Test Game",
                "A browser game for testing.",
                new ArrayList<>(List.of(todo)),
                runDirectory
        );
        run.setGameMetadata(metadata);
        return run;
    }

    private AgentService createService(LlmClient llmClient) {
        return new AgentService(
                new NoOpNotificationSseService(),
                new NoOpTerminalSseService(),
                llmClient,
                tempDir.toString()
        );
    }

    private void invokePrivate(AgentService service, String methodName, AgentRun run) throws Exception {
        Method method = AgentService.class.getDeclaredMethod(methodName, AgentRun.class);
        method.setAccessible(true);
        method.invoke(service, run);
    }

    private static final class NoOpNotificationSseService extends NotificationSseService {
        @Override
        public void sendNotification(String message) {
        }
    }

    private static final class NoOpTerminalSseService extends TerminalSseService {
        @Override
        public void sendTerminalText(String text, SseEventType eventType, int delayMillis) {
        }
    }
}
