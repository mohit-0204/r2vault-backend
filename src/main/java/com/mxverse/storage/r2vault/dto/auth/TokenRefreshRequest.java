package com.mxverse.storage.r2vault.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object for token refresh requests.
 */
public record TokenRefreshRequest(
        @NotBlank(message = "Refresh token is required") String refreshToken
) {
}
