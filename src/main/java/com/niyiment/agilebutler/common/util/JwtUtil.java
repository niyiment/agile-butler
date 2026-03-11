package com.niyiment.agilebutler.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.function.Function;


/**
 * Handles JWT operations such as generation, validation, and claim extraction to maintain stateless authentication.
 */
@Component
@Slf4j
public class JwtUtil {
    private final SecretKey secretKey;
    private final long expirationMs;
    private final long refreshExpirationMs;

    /**
     * Initializes the utility with cryptographic keys and expiration settings from the environment.
     */
    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs,
            @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(
                Base64.getEncoder().encodeToString(secret.getBytes())
        );
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    /**
     * Creates a short-lived access token containing user identity and authorization claims.
     */
    public String generateAccessToken(String email, UUID userId, UUID teamId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", userId);
        claims.put("tid", teamId);
        claims.put("role", role);
        return buildToken(email, claims, expirationMs);
    }

    /**
     * Generates a long-lived refresh token to allow users to obtain new access tokens without re-authenticating.
     */
    public String generateRefreshToken(String email) {
        return buildToken(email, Map.of(), refreshExpirationMs);
    }

    /**
     * Constructs the JWT structure with consistent signatures and timestamps.
     */
    private String buildToken(
            String subject,
            Map<String, Object> extractClaims, long ttlMs
    ) {
        return Jwts.builder()
                .claims(extractClaims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttlMs))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Retrieves the email from the token's subject field to identify the user.
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Applies a mapping function to a claim to retrieve specific data from the token payload.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    /**
     * Fetches the unique user identifier from the token claims.
     */
    public UUID extractUserId(String token) {
        String userId = extractClaim(token, c -> c.get("uid", String.class));
        return userId != null ? UUID.fromString(userId) : null;
    }

    /**
     * Fetches the team identifier to scope operations within a specific team context.
     */
    public UUID extractTeamId(String token) {
        String teamId = extractClaim(token, c -> c.get("tid", String.class));
        return teamId != null ? UUID.fromString(teamId) : null;
    }

    /**
     * Retrieves the user's role to support role-based access control checks.
     */
    public String extractRole(String token) {
        return extractClaim(token, c -> c.get("role", String.class));
    }

    /**
     * Verifies that the token is cryptographically sound and hasn't expired.
     */
    public boolean isValidToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Decodes and verifies the token using the secret key to ensure its integrity.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
