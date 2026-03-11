package com.niyiment.agilebutler.user.dto.request;

import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record UpdateProfileRequest(
        @Size(max = 100) String name,
        String avatarUrl,
        String timezone,
        LocalTime notificationTime,
        String fcmToken
) {
}