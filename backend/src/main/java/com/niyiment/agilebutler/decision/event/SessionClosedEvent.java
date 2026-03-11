package com.niyiment.agilebutler.decision.event;

import java.util.UUID;

public record SessionClosedEvent(
        UUID sessionId,
        String winningOption,
        UUID teamId
) {
}