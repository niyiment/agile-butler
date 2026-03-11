package com.niyiment.agilebutler.standup.event;

import com.niyiment.agilebutler.standup.model.Standup;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Fired after a standup is successfully submitted.
 */
public record StandupSubmittedEvent(
        UUID standupId,
        UUID userId,
        String userName,
        UUID teamId,
        LocalDate date,
        boolean hasBlocker
) {
    public static StandupSubmittedEvent from(Standup standup) {
        return new StandupSubmittedEvent(
                standup.getId(),
                standup.getUser().getId(),
                standup.getUser().getName(),
                standup.getTeam().getId(),
                standup.getStandupDate(),
                standup.isBlockerFlagged()
        );
    }
}