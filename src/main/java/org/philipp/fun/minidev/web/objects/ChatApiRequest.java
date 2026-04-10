package org.philipp.fun.minidev.web.objects;

import java.util.List;

public record ChatApiRequest(
    String message,
    List<HistoryEntry> history
) {
    public record HistoryEntry(
        String role,
        String content,
        String timestamp
    ) {}
}
