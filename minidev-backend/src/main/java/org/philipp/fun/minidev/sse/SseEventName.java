package org.philipp.fun.minidev.sse;

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
