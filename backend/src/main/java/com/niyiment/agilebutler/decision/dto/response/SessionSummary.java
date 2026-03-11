package com.niyiment.agilebutler.decision.dto.response;

import com.niyiment.agilebutler.decision.model.enums.SessionStatus;
import com.niyiment.agilebutler.decision.model.enums.SessionType;

import java.time.Instant;
import java.util.UUID;


public record SessionSummary(
        UUID id,
        String title,
        SessionType sessionType,
        SessionStatus status,
        String winningOption,
        int totalVotes,
        Instant closedAt,
        String category,
        Instant createdAt
) {
}