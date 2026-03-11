package com.niyiment.agilebutler.decision.dto.response;

import java.util.UUID;

public record OptionResponse(
        UUID id,
        String optionText,
        int displayOrder,
        long voteCount,
        double percentage
) {
}