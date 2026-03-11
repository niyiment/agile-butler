package com.niyiment.agilebutler.team.event;

import com.niyiment.agilebutler.user.model.Role;
import java.util.UUID;

public record UserJoinedTeamEvent(UUID userId, UUID teamId, Role role) {
}
