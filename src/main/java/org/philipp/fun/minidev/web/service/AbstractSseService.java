package org.philipp.fun.minidev.web.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractSseService {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final List<String> lastTextTokens = new CopyOnWriteArrayList<>();
    private final Random random = new Random();

    public abstract String getStreamId();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitter.onCompletion(() -> {
            log.info("SSE emitter completed for stream: {}", getStreamId());
            removeEmitter(emitter);
        });

        emitter.onTimeout(() -> {
            log.info("SSE emitter timed out for stream: {}", getStreamId());
            removeEmitter(emitter);
        });

        emitter.onError((ex) -> {
            if (ex instanceof IOException && (ex.getMessage() != null && ex.getMessage().contains("softwaregesteuert"))) {
                log.debug("SSE client disconnected for stream {}: {}", getStreamId(), ex.getMessage());
            } else {
                log.warn("SSE emitter error for stream {}: {}", getStreamId(), ex.getMessage());
            }
            removeEmitter(emitter);
        });

        addEmitter(emitter);
        log.info("New SSE subscriber for stream {}. Total emitters: {}", getStreamId(), emitters.size());

        try {
            emitter.send(SseEmitter.event().name("ping").data(OBJECT_MAPPER.writeValueAsString("connected")));

            if (!lastTextTokens.isEmpty()) {
                for (String token : lastTextTokens) {
                    emitter.send(SseEmitter.event().name("message").data(OBJECT_MAPPER.writeValueAsString(token)));
                }
            }
        } catch (IOException e) {
            log.debug("Failed to send initial data for stream {}: {}", getStreamId(), e.getMessage());
            removeEmitter(emitter);
        }

        return emitter;
    }

    protected synchronized void sendText(String text, String eventType, int delayMillis) {
        log.info("Sending text to {}: eventType={}, length={}", getStreamId(), eventType, text.length());
        if (eventType != null) {
            broadcast("start", eventType);
        }
        lastTextTokens.add(text);

        text.codePoints().forEach(cp -> {
            if (delayMillis > 0) {
                try {
                    Thread.sleep(random.nextInt(delayMillis));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
            String character = Character.toString(cp);
            broadcast("message", character);
        });

        if (eventType != null) {
            broadcast("end", eventType);
        }
    }

    protected synchronized void sendClearCommand() {
        lastTextTokens.clear();
        broadcast("clear", "");
    }

    protected synchronized void deleteLastToken(int delayMillis) {
        if (!lastTextTokens.isEmpty()) {
            String token = lastTextTokens.removeLast();
            StringBuilder sb = new StringBuilder(token);
            String reversed = sb.reverse().toString();
            
            reversed.codePoints().forEach(cp -> {
                if (delayMillis > 0) {
                    try {
                        Thread.sleep(random.nextInt(delayMillis));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }
                String character = Character.toString(cp);
                broadcast("delete", character);
            });
        }
    }

    protected synchronized void broadcast(String name, String data) {

        String jsonData;
        try {
            jsonData = OBJECT_MAPPER.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize SSE data to JSON for stream {}", getStreamId(), e);
            return;
        }

        List<SseEmitter> failedEmitters = new CopyOnWriteArrayList<>();
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name(name).data(jsonData));
            } catch (IOException e) {
                if (e.getMessage() != null && e.getMessage().contains("softwaregesteuert")) {
                    log.debug("SSE client disconnected for stream {}: {}", getStreamId(), e.getMessage());
                } else {
                    log.info("Failed to send event to emitter in stream {}, removing it: {}", getStreamId(), e.getMessage());
                }
                failedEmitters.add(emitter);
            }
        });

        emitters.removeAll(failedEmitters);
    }

    private void addEmitter(SseEmitter emitter) {
        emitters.add(emitter);
    }

    private void removeEmitter(SseEmitter emitter) {
        emitters.remove(emitter);
    }

    @PreDestroy
    public void cleanup() {
        if (emitters.isEmpty()) {
            return;
        }
        log.info("Cleaning up SSE emitters for stream: {}. Closing {} emitters.", getStreamId(), emitters.size());
        emitters.forEach(emitter -> {
            try {
                emitter.complete();
            } catch (Exception e) {
                // Ignore errors during shutdown, especially "recycled facade" errors
                log.debug("Failed to complete emitter during cleanup for stream {}: {}", getStreamId(), e.getMessage());
            }
        });
        emitters.clear();
    }
}
