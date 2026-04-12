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
    
    public enum SseEventName {
        PING("ping"),
        MESSAGE("message"),
        START("start"),
        END("end"),
        CLEAR("clear"),
        DELETE("delete"),
        EVENT("event");

        private final String value;

        SseEventName(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum SseEventType {
        USER_MESSAGE("UserMessage"),
        AGENT_WORK("agent-work"),
        FILE_UPDATE("file-update"),
        FILE_APPEND("file-append"),
        FILE_DELETE("file-delete"),
        SWITCH_TAB("switch-tab");

        private final String value;

        SseEventType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    protected final Logger log = LoggerFactory.getLogger(getClass());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final List<HistoryEvent> history = new CopyOnWriteArrayList<>();
    private final Random random = new Random();

    private record HistoryEvent(SseEventName name, String data, SseEventType type) {}

    public abstract String getStreamId();
    protected abstract boolean isHistoryEnabled();

    public synchronized SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);//TODO

        emitter.onCompletion(() -> {
            log.debug("SSE emitter completed for stream: {}", getStreamId());
            removeEmitter(emitter);
        });

        emitter.onTimeout(() -> {
            log.debug("SSE emitter timed out for stream: {}", getStreamId());
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
            emitter.send(SseEmitter.event().name(SseEventName.PING.getValue()).data(OBJECT_MAPPER.writeValueAsString("connected")));

            if (!history.isEmpty()) {
                for (HistoryEvent event : history) {
                    emitter.send(SseEmitter.event()
                            .name(event.name().getValue())
                            .data(OBJECT_MAPPER.writeValueAsString(event.data())));
                }
            }
        } catch (IOException e) {
            log.debug("Failed to send initial data for stream {}: {}", getStreamId(), e.getMessage());
            removeEmitter(emitter);
        }

        return emitter;
    }

    protected synchronized void sendText(String text, SseEventType eventType, int delayMillis) {
        log.info("Sending text to {}: eventType={}, length={}", getStreamId(), eventType != null ? eventType.getValue() : "null", text.length());
        if (eventType != null) {
            broadcast(SseEventName.START, eventType.getValue());
            if (isHistoryEnabled()) {
                history.add(new HistoryEvent(SseEventName.START, eventType.getValue(), eventType));
            }
        }

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
            broadcast(SseEventName.MESSAGE, character);
            if (isHistoryEnabled()) {
                history.add(new HistoryEvent(SseEventName.MESSAGE, character, eventType));
            }
        });

        if (eventType != null) {
            broadcast(SseEventName.END, eventType.getValue());
            if (isHistoryEnabled()) {
                history.add(new HistoryEvent(SseEventName.END, eventType.getValue(), eventType));
            }
        }
    }

    protected synchronized void sendClearCommand() {
        if (isHistoryEnabled()) {
            history.clear();
        }
        broadcast(SseEventName.CLEAR, "");
    }

    protected synchronized void broadcast(SseEventName eventName, Object data) {
        broadcast(eventName, null, data);
    }

    protected synchronized void broadcast(SseEventName eventName, SseEventType eventType, Object data) {
        if (emitters.isEmpty()) {
            return;
        }

        String jsonData;
        try {
            if (data instanceof String && !isValidJson((String) data)) {
                jsonData = OBJECT_MAPPER.writeValueAsString(data);
            } else if (data instanceof String) {
                jsonData = (String) data;
            } else {
                jsonData = OBJECT_MAPPER.writeValueAsString(data);
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize SSE data to JSON for stream {}", getStreamId(), e);
            return;
        }

        String finalEventName = eventType != null ? eventType.getValue() : eventName.getValue();

        List<SseEmitter> failedEmitters = new CopyOnWriteArrayList<>();
        emitters.forEach(emitter -> {
            try {
                SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event().name(finalEventName).data(jsonData);
                emitter.send(eventBuilder);
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

    private boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }
        String trimmed = json.trim();
        if (!((trimmed.startsWith("{") && trimmed.endsWith("}")) || (trimmed.startsWith("[") && trimmed.endsWith("]")))) {
            return false;
        }
        try {
            OBJECT_MAPPER.readTree(json);
            return true;
        } catch (Exception e) {
            return false;
        }
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
