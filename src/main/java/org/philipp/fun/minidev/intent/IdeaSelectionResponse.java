package org.philipp.fun.minidev.intent;

import java.util.UUID;

public record IdeaSelectionResponse(
        UUID runId,
        int selectedIndex,
        String message
) {
}
