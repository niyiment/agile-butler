package com.niyiment.agilebutler.decision.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AddCommentRequest(
        @NotBlank @Size(max = 1000) String commentText,
        UUID parentCommentId
) {
}