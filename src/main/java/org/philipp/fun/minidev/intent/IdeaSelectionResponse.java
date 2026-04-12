package org.philipp.fun.minidev.intent;

public record IdeaSelectionResponse(
        String runId,
        int selectedIndex,
        String message
) {
}
