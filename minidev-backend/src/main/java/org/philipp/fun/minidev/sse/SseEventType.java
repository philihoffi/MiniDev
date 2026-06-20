package org.philipp.fun.minidev.sse;

/**
 * Enumeration of SSE event types.
 */
public enum SseEventType {

    /** User message event. */
    USER_MESSAGE("UserMessage"),
    /** Agent work event. */
    AGENT_WORK("agent-work"),
    /** File update event. */
    FILE_UPDATE("file-update"),
    /** File append event. */
    FILE_APPEND("file-append"),
    /** File delete event. */
    FILE_DELETE("file-delete"),
    /** Switch tab event. */
    SWITCH_TAB("switch-tab");

    /** The string value of the event type. */
    private final String value;

    /**
     * Constructs an SseEventType.
     *
     * @param value the string value
     */
    SseEventType(String value) {
        this.value = value;
    }

    /**
     * Returns the string value.
     *
     * @return the string value
     */
    public String getValue() {
        return value;
    }
}