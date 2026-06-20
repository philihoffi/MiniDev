package org.philipp.fun.minidev.sse;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PreDestroy;

/**
 * Abstract base class for SSE (Server-Sent Events) services.
 */
public abstract class AbstractSseService {

    /** Shared ObjectMapper for JSON serialization. */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** Logger. */
    protected final Logger log = LoggerFactory.getLogger(getClass());

    /** Active SSE emitters. */
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /** History of sent events for replay on new subscription. */
    private final List<HistoryEvent> history = new CopyOnWriteArrayList<>();

    /**
     * A historical event stored for replay.
     *
     * @param name the event name
     * @param data the event data
     * @param type the event type
     */
    private record HistoryEvent(SseEventName name, String data, SseEventType type) {
    }

    /**
     * Returns the stream identifier.
     *
     * @return the stream ID
     */
    public abstract String getStreamId();

    /**
     * Whether history replay is enabled for this stream.
     *
     * @return true if history is enabled
     */
    protected abstract boolean isHistoryEnabled();

    /**
     * Subscribes a new SSE emitter and replays history.
     *
     * @return the new SSE emitter
     */
    public synchronized SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitter.onCompletion(() -> {
            log.debug("SSE emitter completed for stream: {}", getStreamId());
            removeEmitter(emitter);
        });

        emitter.onTimeout(() -> {
            log.debug("SSE emitter timed out for stream: {}", getStreamId());
            removeEmitter(emitter);
        });

        emitter.onError((ex) -> {
            if (ex instanceof IOException
                    && (ex.getMessage() != null
                    && ex.getMessage().contains("softwaregesteuert"))) {
                log.debug(
                        "SSE client disconnected for stream {}: {}",
                        getStreamId(), ex.getMessage());
            } else {
                log.warn(
                        "SSE emitter error for stream {}: {}",
                        getStreamId(), ex.getMessage());
            }
            removeEmitter(emitter);
        });

        addEmitter(emitter);
        log.info(
                "New SSE subscriber for stream {}. Total emitters: {}",
                getStreamId(), emitters.size());

        try {
            emitter.send(SseEmitter.event()
                    .name(SseEventName.PING.getValue())
                    .data(serializeData("connected")));

            if (!history.isEmpty()) {
                for (HistoryEvent event : history) {
                    try {
                        emitter.send(SseEmitter.event()
                                .name(event.name().getValue())
                                .data(serializeData(event.data())));
                    } catch (IOException e) {
                        log.debug(
                                "Failed to send history event for stream {}: {}",
                                getStreamId(), e.getMessage());
                        break;
                    }
                }
            }
        } catch (IOException e) {
            log.debug(
                    "Failed to send initial data for stream {}: {}",
                    getStreamId(), e.getMessage());
            removeEmitter(emitter);
        }

        return emitter;
    }

    /**
     * Sends text data to all subscribers, optionally wrapped in start/end events.
     *
     * @param text      the text to send
     * @param eventType the event type, or null
     */
    protected void sendText(String text, SseEventType eventType) {
        log.info(
                "Sending text to {}: eventType={}, length={}",
                getStreamId(),
                eventType != null ? eventType.getValue() : "null",
                text.length());

        if (eventType != null) {
            broadcast(SseEventName.START, eventType.getValue());
            if (isHistoryEnabled()) {
                history.add(new HistoryEvent(
                        SseEventName.START, eventType.getValue(), eventType));
            }
        }

        broadcast(SseEventName.MESSAGE, text);
        if (isHistoryEnabled()) {
            history.add(new HistoryEvent(
                    SseEventName.MESSAGE, text, eventType));
        }

        if (eventType != null) {
            broadcast(SseEventName.END, eventType.getValue());
            if (isHistoryEnabled()) {
                history.add(new HistoryEvent(
                        SseEventName.END, eventType.getValue(), eventType));
            }
        }
    }

    /**
     * Sends a clear command to all subscribers.
     */
    protected void sendClearCommand() {
        if (isHistoryEnabled()) {
            history.clear();
        }
        broadcast(SseEventName.CLEAR, "");
    }

    /**
     * Broadcasts an event to all subscribers.
     *
     * @param eventName the event name
     * @param data      the event data
     */
    protected void broadcast(SseEventName eventName, Object data) {
        broadcast(eventName, null, data);
    }

    /**
     * Broadcasts an event with an optional explicit type override.
     *
     * @param eventName the event name
     * @param eventType the event type override, or null
     * @param data      the event data
     */
    protected void broadcast(
            SseEventName eventName, SseEventType eventType, Object data) {
        if (emitters.isEmpty()) {
            return;
        }

        String jsonData;
        try {
            jsonData = serializeData(data);
        } catch (JsonProcessingException e) {
            log.error(
                    "Failed to serialize SSE data to JSON for stream {}",
                    getStreamId(), e);
            return;
        }

        String finalEventName = eventType != null
                ? eventType.getValue() : eventName.getValue();

        List<SseEmitter> failedEmitters = new CopyOnWriteArrayList<>();
        emitters.forEach(emitter -> {
            try {
                SseEmitter.SseEventBuilder eventBuilder =
                        SseEmitter.event().name(finalEventName).data(jsonData);
                emitter.send(eventBuilder);
            } catch (IOException e) {
                if (e.getMessage() != null
                        && e.getMessage().contains("softwaregesteuert")) {
                    log.debug(
                            "SSE client disconnected for stream {}: {}",
                            getStreamId(), e.getMessage());
                } else {
                    log.info(
                            "Failed to send event to emitter in stream {},"
                            + " removing it: {}",
                            getStreamId(), e.getMessage());
                }
                failedEmitters.add(emitter);
            }
        });

        emitters.removeAll(failedEmitters);
    }

    /**
     * Checks whether a string is valid JSON.
     *
     * @param json the string to check
     * @return true if valid JSON
     */
    private boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }
        try {
            OBJECT_MAPPER.readTree(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Serializes data to a JSON string. If the data is already a valid JSON string,
     * returns it as-is.
     *
     * @param data the data to serialize
     * @return the JSON string
     * @throws JsonProcessingException if serialization fails
     */
    private String serializeData(Object data) throws JsonProcessingException {
        if (data instanceof String str && isValidJson(str)) {
            return str;
        }
        return OBJECT_MAPPER.writeValueAsString(data);
    }

    /**
     * Adds an emitter to the active list.
     *
     * @param emitter the emitter to add
     */
    private void addEmitter(SseEmitter emitter) {
        emitters.add(emitter);
    }

    /**
     * Removes an emitter from the active list.
     *
     * @param emitter the emitter to remove
     */
    private void removeEmitter(SseEmitter emitter) {
        emitters.remove(emitter);
    }

    /**
     * Cleans up all active emitters on shutdown.
     */
    @PreDestroy
    public void cleanup() {
        if (emitters.isEmpty()) {
            return;
        }
        log.info(
                "Cleaning up SSE emitters for stream: {}. Closing {} emitters.",
                getStreamId(), emitters.size());
        emitters.forEach(emitter -> {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.debug(
                        "Failed to complete emitter during cleanup"
                        + " for stream {}: {}",
                        getStreamId(), e.getMessage());
            }
        });
        emitters.clear();
    }
}