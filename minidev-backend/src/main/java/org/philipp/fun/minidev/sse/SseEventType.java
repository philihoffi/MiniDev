package org.philipp.fun.minidev.sse;

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
