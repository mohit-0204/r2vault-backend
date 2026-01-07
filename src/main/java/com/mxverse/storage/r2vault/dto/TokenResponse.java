package com.mxverse.storage.r2vault.dto;

/**
 * Response object containing Access and Refresh tokens.
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String type) {
    public TokenResponse(String accessToken, String refreshToken) {
        this(accessToken, refreshToken, "Bearer");
    }
}
