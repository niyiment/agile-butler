package com.niyiment.agilebutler.decision.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;


public record VoteUpdateMessage(
        UUID sessionId,
        List<OptionResponse> options,
        int totalVotes,
        int participantCount,
        Instant timestamp
) {
}