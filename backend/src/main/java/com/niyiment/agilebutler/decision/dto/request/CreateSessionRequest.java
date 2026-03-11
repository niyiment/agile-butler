package com.niyiment.agilebutler.decision.dto.request;

import com.niyiment.agilebutler.decision.model.enums.DecisionType;
import com.niyiment.agilebutler.decision.model.enums.SessionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateSessionRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        @NotNull SessionType sessionType,
        @NotNull DecisionType decisionType,
        @NotEmpty @Size(min = 2, max = 10) List<@NotBlank String> options,
        boolean anonymous,
        Integer maxDurationMinutes,
        String category
) {
}




