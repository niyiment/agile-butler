package com.niyiment.agilebutler.common.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private final String secret = "f2b2d30a0aa7a886fd059de9b181a180fe6dce07dfebcc6bea6e8427954b5c2e";
    private final long expirationMs = 3600000;
    private final long refreshExpirationMs = 86400000;
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(secret, expirationMs, refreshExpirationMs);
    }

    @Test
    void shouldGenerateValidAccessToken() {
        String email = "user@example.com";
        UUID userId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        String role = "ADMIN";

        String token = jwtUtil.generateAccessToken(email, userId, teamId, role);

        assertThat(token).isNotNull();
        assertThat(jwtUtil.isValidToken(token)).isTrue();
        assertThat(jwtUtil.extractEmail(token)).isEqualTo(email);
        assertThat(jwtUtil.extractRole(token)).isEqualTo(role);
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(userId);
        assertThat(jwtUtil.extractTeamId(token)).isEqualTo(teamId);
    }

    @Test
    void shouldGenerateValidRefreshToken() {
        String email = "user@example.com";

        String token = jwtUtil.generateRefreshToken(email);

        assertThat(token).isNotNull();
        assertThat(jwtUtil.isValidToken(token)).isTrue();
        assertThat(jwtUtil.extractEmail(token)).isEqualTo(email);
    }

    @Test
    void shouldFailForInvalidToken() {
        String invalidToken = "invalid.token.here";
        assertThat(jwtUtil.isValidToken(invalidToken)).isFalse();
    }

    @Test
    void shouldFailForExpiredToken() throws InterruptedException {
        JwtUtil shortLivedJwtUtil = new JwtUtil(secret, 1, refreshExpirationMs);
        String token = shortLivedJwtUtil.generateAccessToken("user@example.com", UUID.randomUUID(), UUID.randomUUID(), "USER");

        // Wait for token to expire
        Thread.sleep(100);

        assertThat(shortLivedJwtUtil.isValidToken(token)).isFalse();
    }
}
