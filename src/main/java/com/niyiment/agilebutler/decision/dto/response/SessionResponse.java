package com.niyiment.agilebutler.decision.dto.response;

import com.niyiment.agilebutler.decision.model.enums.DecisionType;
import com.niyiment.agilebutler.decision.model.enums.SessionStatus;
import com.niyiment.agilebutler.decision.model.enums.SessionType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        String title,
        String description,
        UUID teamId,
        UUID createdById,
        String createdByName,
        SessionType sessionType,
        DecisionType decisionType,
        SessionStatus status,
        boolean anonymous,
        String category,
        Instant closesAt,
        Instant closedAt,
        List<OptionResponse> options,
        int totalVotes,
        int participantCount,
        Instant createdAt
) {
}