package com.niyiment.agilebutler.standup.dto.response;

import com.niyiment.agilebutler.standup.model.Standup;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record StandupResponse(
        UUID id,
        UUID userId,
        String userName,
        String userAvatarUrl,
        LocalDate standupDate,
        String yesterdayText,
        String todayText,
        String blockersText,
        boolean blockerFlagged,
        String status,
        List<AttachmentResponse> attachments,
        Instant submittedAt
) {
    public static StandupResponse from(Standup s) {
        return new StandupResponse(
                s.getId(),
                s.getUser().getId(),
                s.getUser().getName(),
                s.getUser().getAvatarUrl(),
                s.getStandupDate(),
                s.getYesterdayText(),
                s.getTodayText(),
                s.getBlockersText(),
                s.isBlockerFlagged(),
                s.getStatus().name(),
                s.getAttachments().stream().map(AttachmentResponse::from).toList(),
                s.getCreatedAt()
        );
    }
}


