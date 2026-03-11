package com.niyiment.agilebutler.decision.dto.response;

import java.util.UUID;

public record CommentMessage(
        UUID sessionId,
        CommentResponse comment
) {
}