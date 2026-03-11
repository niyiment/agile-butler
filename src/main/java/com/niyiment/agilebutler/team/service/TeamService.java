package com.niyiment.agilebutler.team.service;


import com.niyiment.agilebutler.common.exception.BusinessException;
import com.niyiment.agilebutler.common.exception.ResourceNotFoundException;
import com.niyiment.agilebutler.team.dto.request.CreateTeamRequest;
import com.niyiment.agilebutler.team.dto.request.JoinTeamRequest;
import com.niyiment.agilebutler.team.dto.request.UpdateTeamRequest;
import com.niyiment.agilebutler.team.dto.response.TeamResponse;
import com.niyiment.agilebutler.team.event.UserJoinedTeamEvent;
import com.niyiment.agilebutler.team.model.Team;
import com.niyiment.agilebutler.team.repository.TeamRepository;
import com.niyiment.agilebutler.user.model.Role;
import com.niyiment.agilebutler.user.model.User;
import com.niyiment.agilebutler.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Handles team creation, membership, and configuration to facilitate collaborative project management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamService {
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom random = new SecureRandom();
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Initializes a new team and assigns the creator as the Scrum Master to establish initial leadership.
     */
    @Transactional
    public TeamResponse createTeam(UUID creatorId, CreateTeamRequest request) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", creatorId));

        Team team = Team.builder()
                .name(request.name())
                .description(request.description())
                .timezone(request.timezone() != null ? request.timezone() : "UTC")
                .standupDeadlineTime(request.standupDeadlineTime() != null ?
                        request.standupDeadlineTime() : LocalTime.of(11, 0))
                .inviteCode(generateUniqueInviteCode())
                .build();
        team = teamRepository.save(team);

        eventPublisher.publishEvent(new UserJoinedTeamEvent(creator.getId(), team.getId(), Role.SCRUM_MASTER));

        log.info("Team '{}' created by {}", team.getName(), creator.getEmail());
        return TeamResponse.from(team);
    }

    /**
     * Updates team settings such as name or standup deadlines to reflect changes in team structure or process.
     */
    @Transactional
    public TeamResponse updateTeam(UUID id, UpdateTeamRequest request) {
        Team team = findOrThrow(id);

        if (request.name() != null) team.setName(request.name());
        if (request.description() != null) team.setDescription(request.description());
        if (request.timezone() != null) team.setTimezone(request.timezone());
        if (request.standupDeadlineTime() != null) team.setStandupDeadlineTime(request.standupDeadlineTime());

        return TeamResponse.from(teamRepository.save(team));
    }

    /**
     * Adds a user to a team using a valid invite code to expand the team's membership.
     */
    @Transactional
    public TeamResponse joinTeam(UUID id, JoinTeamRequest request) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));

        if (user.getTeam() != null) {
            throw new BusinessException("User is already a member of a team");
        }

        Team team = teamRepository.findByInviteCode(request.inviteCode().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Invite code", request.inviteCode()));
        
        eventPublisher.publishEvent(new UserJoinedTeamEvent(user.getId(), team.getId(), Role.MEMBER));
        
        log.info("User {} joined team '{}'", user.getName(), team.getName());

        return TeamResponse.from(team);
    }

    /**
     * Fetches details for a specific team by its identifier.
     */
    @Transactional
    public TeamResponse getById(UUID teamId) {
        return TeamResponse.from(findOrThrow(teamId));
    }

    /**
     * Generates a new unique invite code for the team to control and refresh access security.
     */
    @Transactional
    public String regenerateInviteCode(UUID teamId) {
        Team team = findOrThrow(teamId);
        team.setInviteCode(generateUniqueInviteCode());
        teamRepository.save(team);

        return team.getInviteCode();
    }

    /**
     * Finds a team by ID or throws an exception to ensure only valid teams are processed.
     */
    public Team findOrThrow(UUID id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team", id));
    }

    /**
     * Ensures every new invite code is unique within the system to prevent collisions.
     */
    private String generateUniqueInviteCode() {
        String code;
        do {
            code = generateCode();
        } while (teamRepository.findByInviteCode(code).isPresent());
        return code;
    }

    /**
     * Creates a random alphanumeric string to serve as a secure invitation identifier.
     */
    private String generateCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}