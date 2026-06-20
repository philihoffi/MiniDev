package org.philipp.fun.minidev.sse;

/**
 * Enumeration of SSE event names.
 */
public enum SseEventName {

    /** Ping event. */
    PING("ping"),
    /** Message event. */
    MESSAGE("message"),
    /** Start event. */
    START("start"),
    /** End event. */
    END("end"),
    /** Clear event. */
    CLEAR("clear"),
    /** Delete event. */
    DELETE("delete"),
    /** Event. */
    EVENT("event");

    /** The string value of the event name. */
    private final String value;

    /**
     * Constructs an SseEventName.
     *
     * @param value the string value
     */
    SseEventName(String value) {
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