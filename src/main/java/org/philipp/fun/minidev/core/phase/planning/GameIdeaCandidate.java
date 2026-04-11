package org.philipp.fun.minidev.core.phase.planning;

public record GameIdeaCandidate(
        String name,
        String hook,
        String coreMechanic,
        String uniqueness,
        String similarityRisk,
        int feasibility,
        int originalityScore
) {
}
