package org.philipp.fun.minidev.core.phase.reviewing;

import java.util.List;

public record ReviewResponse(
        List<String> failedDoneTodos,
        List<String> newTodos,
        List<String> consolidatedTodos,
        String reviewSummary
) {}
