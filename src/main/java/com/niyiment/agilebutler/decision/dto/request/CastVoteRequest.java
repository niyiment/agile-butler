package com.niyiment.agilebutler.decision.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CastVoteRequest(
        @NotNull UUID optionId,
        String commentText,
        Integer rankOrder
) {
}