package com.niyiment.agilebutler.standup.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Aggregated view of a team's standup for a given day,
 * shown on the Manager Dashboard.
 */
public record TeamStandupSummary(
        LocalDate date,
        int totalMembers,
        int submitted,
        int pending,
        List<String> missingMembers,
        List<StandupResponse> standups,
        List<StandupResponse> blockers,
        double participationRate
) {
}