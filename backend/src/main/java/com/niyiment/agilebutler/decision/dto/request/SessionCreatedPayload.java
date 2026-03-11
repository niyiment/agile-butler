package com.niyiment.agilebutler.decision.dto.request;

import java.util.UUID;

public record SessionCreatedPayload(UUID sessionId, String title, UUID teamId, UUID createdById) {
}
