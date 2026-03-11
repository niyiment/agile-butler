package com.niyiment.agilebutler.user.dto.response;

import com.niyiment.agilebutler.user.model.User;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
    public static AuthResponse of(String access, String refresh, long expiresIn, User user) {
        return new AuthResponse(access, refresh, "Bearer", expiresIn, UserResponse.from(user));
    }
}