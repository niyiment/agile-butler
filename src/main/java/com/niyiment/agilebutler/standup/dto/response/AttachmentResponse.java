package com.niyiment.agilebutler.standup.dto.response;

import com.niyiment.agilebutler.standup.model.StandupAttachment;
import com.niyiment.agilebutler.standup.model.enums.AttachmentType;

import java.time.Instant;
import java.util.UUID;

public record AttachmentResponse(
        UUID id,
        AttachmentType attachmentType,
        String url,
        String label,
        String thumbnailUrl,
        Instant createdAt
) {
    public static AttachmentResponse from(StandupAttachment a) {
        return new AttachmentResponse(
                a.getId(), a.getAttachmentType(),
                a.getUrl(), a.getLabel(),
                a.getThumbnailUrl(), a.getCreatedAt()
        );
    }
}
