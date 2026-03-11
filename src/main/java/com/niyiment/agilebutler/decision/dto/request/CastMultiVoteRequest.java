package com.niyiment.agilebutler.decision.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record CastMultiVoteRequest(
        @NotEmpty List<UUID> optionIds,
        String commentText
) {
}