package com.niyiment.agilebutler.team.dto.response;

import com.niyiment.agilebutler.team.model.Team;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record TeamResponse(
        UUID id,
        String name,
        String description,
        LocalTime standupDeadlineTime,
        String timezone,
        String inviteCode,
        int memberCount,
        Instant createdAt
) {
    public static TeamResponse from(Team team) {
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getDescription(),
                team.getStandupDeadlineTime(),
                team.getTimezone(),
                team.getInviteCode(),
                team.getMembers().size(),
                team.getCreatedAt()
        );
    }
}