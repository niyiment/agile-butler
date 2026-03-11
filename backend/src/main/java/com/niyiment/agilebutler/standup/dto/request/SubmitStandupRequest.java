package com.niyiment.agilebutler.standup.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SubmitStandupRequest(
        @NotBlank String yesterdayText,
        @NotBlank String todayText,
        String blockersText           // optional
) {
}


