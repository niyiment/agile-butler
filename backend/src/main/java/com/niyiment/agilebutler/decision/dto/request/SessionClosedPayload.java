package com.niyiment.agilebutler.decision.dto.request;

import java.util.UUID;

public record SessionClosedPayload(UUID sessionId, String winner) {
}