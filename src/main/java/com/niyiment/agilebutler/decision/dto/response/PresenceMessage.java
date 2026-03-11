package com.niyiment.agilebutler.decision.dto.response;

import java.util.UUID;


public record PresenceMessage(
        UUID sessionId,
        UUID userId,
        String userName,
        String userAvatarUrl,
        boolean joined,
        int currentCount
) {
}