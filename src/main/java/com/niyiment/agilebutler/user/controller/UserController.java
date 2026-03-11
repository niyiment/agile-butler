package com.niyiment.agilebutler.user.controller;


import com.niyiment.agilebutler.common.model.ApiResponse;
import com.niyiment.agilebutler.user.dto.request.*;
import com.niyiment.agilebutler.user.dto.response.AuthResponse;
import com.niyiment.agilebutler.user.dto.response.UserResponse;
import com.niyiment.agilebutler.user.model.User;
import com.niyiment.agilebutler.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "User & Auth", description = "Authentication and user profile management")
public class UserController {
    private final UserService userService;

    // --- Auth ----
    @PostMapping("/auth/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(userService.register(request)));
    }

    @PostMapping("/auth/login")
    @Operation(summary = "Login and access token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(userService.login(request)));
    }

    @PostMapping("/auth/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(userService.refresh(request)));
    }

    // -- User Profile ----
    @GetMapping("/user/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(UserResponse.from(user)));
    }

    @PatchMapping("/user/me")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.updateProfile(user.getId(), request)));
    }

    @PatchMapping("/user/me/password")
    @Operation(summary = "Change password")
    public ResponseEntity<ApiResponse<UserResponse>> changePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully", null));
    }

    @GetMapping("/teams/{teamId}/members")
    @PreAuthorize("hasAnyRole('ADMIN', 'SCRUM_MASTER')")
    @Operation(summary = "List team members")
    public ResponseEntity<ApiResponse<List<UserResponse>>> teamMembers(
            @PathVariable UUID teamId) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getTeamMembers(teamId)));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getById(id)));
    }
}
