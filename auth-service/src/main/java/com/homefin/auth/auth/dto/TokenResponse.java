package com.homefin.auth.auth.dto;

/**
 * JSON response of a successful login:
 * {@code {"accessToken": "eyJ...", "tokenType": "Bearer", "expiresIn": 900}} (OAuth2-style field names).
 *
 * <p>An immutable record, like a TS {@code type TokenResponse = Readonly<{...}>}.
 */
public record TokenResponse(String accessToken, String tokenType, long expiresIn) {

    // Static factory method: a named alternative to calling the constructor, fixing tokenType.
    public static TokenResponse bearer(String token, long expiresInSeconds) {
        return new TokenResponse(token, "Bearer", expiresInSeconds);
    }
}
