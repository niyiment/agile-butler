package com.niyiment.agilebutler.team.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record CreateTeamRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Maximum length is 100 characters")
        String name,
        @Size(max = 500, message = "Maximum length is 500 characters")
        String description,
        LocalTime standupDeadlineTime,
        String timezone
) {
}




