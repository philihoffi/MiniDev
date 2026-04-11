package org.philipp.fun.minidev.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseService {

    private final Logger log = LoggerFactory.getLogger(SseService.class);
    
    public enum StreamType {
        TERMINAL,
        NOTIFICATIONS,
        SYSTEM
    }

    private final Map<StreamType, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final Map<StreamType, List<String>> lastTextTokens = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public SseEmitter subscribe(StreamType streamId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitter.onCompletion(() -> {
            log.info("SSE emitter completed for stream: {}", streamId);
            removeEmitter(streamId, emitter);
        });

        emitter.onTimeout(() -> {
            log.info("SSE emitter timed out for stream: {}", streamId);
            removeEmitter(streamId, emitter);
        });

        emitter.onError((ex) -> {
            log.error("SSE emitter error for stream {}: {}", streamId, ex.getMessage());
            removeEmitter(streamId, emitter);
        });

        addEmitter(streamId, emitter);
        log.info("New SSE subscriber for stream {}. Total streams: {}", streamId, emitters.size());

        try {
            emitter.send(SseEmitter.event().name("ping").data("connected"));

            List<String> tokens = lastTextTokens.get(streamId);
            if (tokens != null && !tokens.isEmpty()) {
                for (String token : tokens) {
                    emitter.send(SseEmitter.event().name("message").data(token));
                }
            }
        } catch (IOException e) {
            log.error("Failed to send initial data for stream {}", streamId, e);
            removeEmitter(streamId, emitter);
        }

        return emitter;
    }

    @Async
    public synchronized void sendText(StreamType streamId, String text) {
        lastTextTokens.computeIfAbsent(streamId, k -> new CopyOnWriteArrayList<>()).add(text);
        text.chars().forEach(c -> {
            try {
                Thread.sleep(random.nextInt(100));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
            broadcast(streamId, "message", (char) c);
        });
    }

    public synchronized void sendClearCommand(StreamType streamId) {
        List<String> tokens = lastTextTokens.get(streamId);
        if (tokens != null) {
            tokens.clear();
        }
        broadcast(streamId, "clear", "");
    }

    @Async
    public synchronized void deleteLastToken(StreamType streamId) {
        List<String> tokens = lastTextTokens.get(streamId);
        if (tokens != null && !tokens.isEmpty()) {
            String token = tokens.removeLast();
            token.chars().forEach(c -> {
                try {
                    Thread.sleep(random.nextInt(100));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
                broadcast(streamId, "delete", (char) c);
            });
        }
    }

    private void broadcast(StreamType streamId, String name, char data) {
        broadcast(streamId, name, String.valueOf(data));
    }

    private synchronized void broadcast(StreamType streamId, String name, String data) {
        log.info("Broadcasting SSE event to {}: name={}, data={}", streamId, name, data);

        List<SseEmitter> streamEmitters = emitters.get(streamId);
        if (streamEmitters == null) return;

        List<SseEmitter> failedEmitters = new CopyOnWriteArrayList<>();
        streamEmitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name(name).data(data));
            } catch (IOException e) {
                log.warn("Failed to send event to emitter in stream {}, removing it: {}", streamId, e.getMessage());
                failedEmitters.add(emitter);
            }
        });

        streamEmitters.removeAll(failedEmitters);
    }

    private void addEmitter(StreamType streamId, SseEmitter emitter) {
        emitters.computeIfAbsent(streamId, k -> new CopyOnWriteArrayList<>()).add(emitter);
    }

    private void removeEmitter(StreamType streamId, SseEmitter emitter) {
        List<SseEmitter> streamEmitters = emitters.get(streamId);
        if (streamEmitters != null) {
            streamEmitters.remove(emitter);
            if (streamEmitters.isEmpty()) {
                emitters.remove(streamId);
            }
        }
    }
}
