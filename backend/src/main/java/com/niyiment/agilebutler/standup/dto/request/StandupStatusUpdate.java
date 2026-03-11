package com.niyiment.agilebutler.standup.dto.request;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Real-time status update pushed over WebSocket when a member submits.
 */
public record StandupStatusUpdate(
        UUID teamId,
        LocalDate date,
        UUID userId,
        String userName,
        boolean submitted,
        int totalSubmitted,
        int totalMembers
) {
}