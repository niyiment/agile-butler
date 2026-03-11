package com.niyiment.agilebutler.decision.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CastRankedVoteRequest(
        @NotEmpty
        List<@NotNull UUID> rankedOptionIds
) {
}
