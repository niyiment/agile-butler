package com.niyiment.agilebutler.decision.dto.response;

import com.niyiment.agilebutler.decision.model.Comment;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID userId,
        String userName,
        String userAvatarUrl,
        String commentText,
        UUID parentCommentId,
        int reactionCount,
        Map<String, String> userReactions,
        Instant createdAt
) {
    public static CommentResponse from(Comment c) {
        return new CommentResponse(
                c.getId(),
                c.getUser().getId(),
                c.getUser().getName(),
                c.getUser().getAvatarUrl(),
                c.getCommentText(),
                c.getParentCommentId(),
                c.getReactionCount(),
                c.getUserReactions(),
                c.getCreatedAt()
        );
    }
}