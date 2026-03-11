package com.niyiment.agilebutler.decision.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReactToCommentRequest(
        @NotBlank
        @Size(max = 10) String emoji
) {
}
