package com.niyiment.agilebutler.user.dto.response;

import com.niyiment.agilebutler.user.model.User;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        String avatarUrl,
        String role,
        String timezone,
        LocalTime notificationTime,
        UUID teamId,
        String teamName,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getRole().name(),
                user.getTimezone(),
                user.getNotificationTime(),
                user.getTeam() != null ? user.getTeam().getId() : null,
                user.getTeam() != null ? user.getTeam().getName() : null,
                user.getCreatedAt()
        );
    }
}