package com.niyiment.agilebutler.decision.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateFromBlockerRequest(
        @NotNull
        UUID standupId,
        @NotBlank
        @Size(max = 500)
        String blockerText,
        @Size(max = 2000)
        String blockerContext
) {
}
