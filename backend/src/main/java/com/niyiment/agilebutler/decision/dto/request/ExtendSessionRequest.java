package com.niyiment.agilebutler.decision.dto.request;

import jakarta.validation.constraints.NotNull;

public record ExtendSessionRequest(
        @NotNull
        Integer additionalMinutes
) {
}
