package com.niyiment.agilebutler.team.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinTeamRequest(
        @NotBlank(message = "Invite code is required")
        @Size(min = 6, max = 12, message = "Invite code must be between 6 and 12 characters")
        String inviteCode
) {
}