package com.niyiment.agilebutler.standup.dto.request;

public record SaveDraftRequest(
        String yesterdayText,
        String todayText,
        String blockersText
) {
}