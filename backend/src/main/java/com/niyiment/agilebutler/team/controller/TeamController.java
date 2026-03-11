package com.niyiment.agilebutler.team.controller;


import com.niyiment.agilebutler.common.model.ApiResponse;
import com.niyiment.agilebutler.team.dto.request.CreateTeamRequest;
import com.niyiment.agilebutler.team.dto.request.JoinTeamRequest;
import com.niyiment.agilebutler.team.dto.request.UpdateTeamRequest;
import com.niyiment.agilebutler.team.dto.response.TeamResponse;
import com.niyiment.agilebutler.team.service.TeamService;
import com.niyiment.agilebutler.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
@Tag(name = "Teams", description = "Team management")
public class TeamController {
    private final TeamService teamService;

    @PostMapping
    @Operation(summary = "Create a new team")
    public ResponseEntity<ApiResponse<TeamResponse>> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateTeamRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(teamService.createTeam(user.getId(), request)));
    }

    @GetMapping("/{teamId}")
    @Operation(summary = "Get team by ID")
    public ResponseEntity<ApiResponse<TeamResponse>> getById(@PathVariable UUID teamId) {
        return ResponseEntity.ok(ApiResponse.ok(teamService.getById(teamId)));
    }

    @PatchMapping("/{teamId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SCRUM_MASTER')")
    @Operation(summary = "Update team settings")
    public ResponseEntity<ApiResponse<TeamResponse>> update(
            @PathVariable UUID teamId,
            @Valid @RequestBody UpdateTeamRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(teamService.updateTeam(teamId, request)));
    }

    @PostMapping("/join")
    @Operation(summary = "Join a team using invite code")
    public ResponseEntity<ApiResponse<TeamResponse>> join(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody JoinTeamRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(teamService.joinTeam(user.getId(), request)));
    }

    @PostMapping("/{teamId}/invite-code/regenerate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SCRUM_MASTER')")
    @Operation(summary = "Regenerate team invite code")
    public ResponseEntity<ApiResponse<String>> regenerateInviteCode(
            @PathVariable UUID teamId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(teamService.regenerateInviteCode(teamId)));
    }
}