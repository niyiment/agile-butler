package com.niyiment.agilebutler.user.service;


import com.niyiment.agilebutler.common.exception.BusinessException;
import com.niyiment.agilebutler.common.exception.ResourceNotFoundException;
import com.niyiment.agilebutler.common.util.JwtUtil;
import com.niyiment.agilebutler.team.event.UserJoinedTeamEvent;
import com.niyiment.agilebutler.team.repository.TeamRepository;
import com.niyiment.agilebutler.user.dto.request.*;
import com.niyiment.agilebutler.user.dto.response.AuthResponse;
import com.niyiment.agilebutler.user.dto.response.UserResponse;
import com.niyiment.agilebutler.user.model.Role;
import com.niyiment.agilebutler.user.model.User;
import com.niyiment.agilebutler.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Manages user accounts, authentication flow, and team assignments to ensure secure and organized access.
 */
@Slf4j
@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    /**
     * Initializes the service with necessary dependencies for user management and security.
     */
    public UserService(UserRepository userRepository, TeamRepository teamRepository, PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       @Lazy AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Loads user details by email to support Spring Security's authentication process.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + email));
    }

    /**
     * Creates a new user account and returns authentication tokens for immediate access.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email already registered: " + request.email());
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .timezone(request.timezone() != null ? request.timezone() : "UTC")
                .role(Role.MEMBER)
                .build();
        user = userRepository.save(user);
        log.info("New user registered with email: {}", request.email());

        return buildAuthResponse(user);
    }

    /**
     * Validates user credentials and issues JWT tokens upon successful authentication.
     */
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.email()));

        return buildAuthResponse(user);
    }

    /**
     * Issues new authentication tokens using a valid refresh token to maintain a continuous user session.
     */
    public AuthResponse refresh(RefreshTokenRequest request) {
        String token = request.refreshToken();
        if (!jwtUtil.isValidToken(token)) {
            throw new BusinessException("Invalid or expired refresh token");
        }
        String email = jwtUtil.extractEmail(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        return buildAuthResponse(user);
    }

    /**
     * Retrieves basic user profile information by unique identifier.
     */
    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return UserResponse.from(findOrThrow(id));
    }

    /**
     * Updates user profile details such as name, timezone, and notification preferences.
     */
    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findOrThrow(userId);

        if (request.name() != null) user.setName(request.name());
        if (request.avatarUrl() != null) user.setAvatarUrl(request.avatarUrl());
        if (request.timezone() != null) user.setTimezone(request.timezone());
        if (request.notificationTime() != null) user.setNotificationTime(request.notificationTime());
        if (request.fcmToken() != null) user.setFcmToken(request.fcmToken());

        return UserResponse.from(userRepository.save(user));
    }

    /**
     * Secures the account by allowing users to update their password after verifying the current one.
     */
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = findOrThrow(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException("Current password is not correct");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        log.info("Change password for user: {}", user.getEmail());
    }

    /**
     * Responds to team membership changes by updating the user's team affiliation and role.
     */
    @EventListener
    @Transactional
    public void handleUserJoinedTeam(UserJoinedTeamEvent event) {
        User user = findOrThrow(event.userId());
        var team = teamRepository.findById(event.teamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team", event.teamId()));
        
        user.setTeam(team);
        user.setRole(event.role());
        userRepository.save(user);
        log.info("Processed team assignment for user {} to team {}", user.getEmail(), team.getName());
    }

    /**
     * Lists all active members belonging to a specific team for collaboration purposes.
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getTeamMembers(UUID teamId) {
        return userRepository.findAllByTeamIdAndActiveTrue(teamId)
                .stream()
                .map(UserResponse::from)
                .toList();

    }

    /**
     * Finds a user by ID or throws an exception if the user does not exist.
     */
    public User findOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    /**
     * Constructs a unified authentication response containing both access and refresh tokens.
     */
    private AuthResponse buildAuthResponse(User user) {
        UUID teamId = user.getTeam() != null ? user.getTeam().getId() : null;
        String access = jwtUtil.generateAccessToken(
                user.getEmail(), user.getId(), teamId, user.getRole().name()
        );
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());
        return AuthResponse.of(access, refreshToken,
                24 * 60 * 60 * 1000L, user);
    }
}
