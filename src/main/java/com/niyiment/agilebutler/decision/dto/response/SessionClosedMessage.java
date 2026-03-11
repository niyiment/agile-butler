package com.niyiment.agilebutler.decision.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;


public record SessionClosedMessage(
        UUID sessionId,
        String winningOption,
        List<OptionResponse> finalResults,
        int totalVotes,
        Instant closedAt
) {
}