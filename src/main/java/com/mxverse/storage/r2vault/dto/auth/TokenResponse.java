package com.mxverse.storage.r2vault.dto.auth;

import lombok.Builder;

/**
 * Data Transfer Object representing the authentication tokens returned after login or refresh.
 * <p>
 * Contains the access token for authorizing requests and the refresh token
 * used to obtain new access tokens when they expire.
 *
 * @param accessToken  The JWT used for immediate authentication.
 * @param refreshToken The token used for session extension.
 * @param type         The token type (usually "Bearer").
 * @param accountKey   Cryptographic metadata for the user's account key.
 */
@Builder
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String type,
        AccountKeyMetadataDto accountKey) {

    public TokenResponse(String accessToken, String refreshToken) {
        this(accessToken, refreshToken, "Bearer", null);
    }
}
