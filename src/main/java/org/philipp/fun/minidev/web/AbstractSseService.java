package org.philipp.fun.minidev.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractSseService {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    
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
            log.error("SSE emitter error for stream {}: {}", getStreamId(), ex.getMessage());
            removeEmitter(emitter);
        });

        addEmitter(emitter);
        log.info("New SSE subscriber for stream {}. Total emitters: {}", getStreamId(), emitters.size());

        try {
            emitter.send(SseEmitter.event().name("ping").data("connected"));

            if (!lastTextTokens.isEmpty()) {
                for (String token : lastTextTokens) {
                    emitter.send(SseEmitter.event().name("message").data(token));
                }
            }
        } catch (IOException e) {
            log.error("Failed to send initial data for stream {}", getStreamId(), e);
            removeEmitter(emitter);
        }

        return emitter;
    }

    protected synchronized void sendText(String text, String eventType) {
        if (eventType != null) {
            broadcast("start", eventType);
        }
        lastTextTokens.add(text);
        text.chars().forEach(c -> {
            try {
                Thread.sleep(random.nextInt(100));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
            broadcast("message", (char) c);
        });
        if (eventType != null) {
            broadcast("end", eventType);
        }
    }

    protected synchronized void sendClearCommand() {
        lastTextTokens.clear();
        broadcast("clear", "");
    }

    protected synchronized void deleteLastToken() {
        if (!lastTextTokens.isEmpty()) {
            String token = lastTextTokens.removeLast();
            token.chars().forEach(c -> {
                try {
                    Thread.sleep(random.nextInt(100));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
                broadcast("delete", (char) c);
            });
        }
    }

    protected void broadcast(String name, char data) {
        broadcast(name, String.valueOf(data));
    }

    protected synchronized void broadcast(String name, String data) {
        log.info("Broadcasting SSE event to {}: name={}, data={}", getStreamId(), name, data);

        List<SseEmitter> failedEmitters = new CopyOnWriteArrayList<>();
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name(name).data(data));
            } catch (IOException e) {
                log.warn("Failed to send event to emitter in stream {}, removing it: {}", getStreamId(), e.getMessage());
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
}
