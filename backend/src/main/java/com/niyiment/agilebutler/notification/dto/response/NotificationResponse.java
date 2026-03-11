package com.niyiment.agilebutler.notification.dto.response;

import com.niyiment.agilebutler.notification.model.Notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String type,
        String title,
        String body,
        UUID referenceId,
        boolean read,
        Instant readAt,
        Instant createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getType().name(),
                n.getTitle(), n.getBody(), n.getReferenceId(),
                n.isRead(), n.getReadAt(), n.getCreatedAt()
        );
    }
}