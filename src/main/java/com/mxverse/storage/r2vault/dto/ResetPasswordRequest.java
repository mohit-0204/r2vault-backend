package com.mxverse.storage.r2vault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for password reset requests.
 *
 * @param username           The username of the account to reset.
 * @param newPassword        The new password to set (must meet safety constraints).
 * @param wrappedAccountKey  The new Account Key wrapped with the new password-derived key.
 * @param kdfSalt            The salt used for the new key derivation.
 * @param kdfIterations      The benchmarked iterations used for the new key derivation.
 */
public record ResetPasswordRequest(
        @NotBlank(message = "Username is required") String username,
        @NotBlank(message = "New password is required") @Size(min = 6, message = "Password must be at least 6 characters") String newPassword,
        @NotBlank(message = "New wrapped account key is required") String wrappedAccountKey,
        @NotBlank(message = "New KDF salt is required") String kdfSalt,
        @NotNull(message = "New KDF iterations are required") Integer kdfIterations
) {
}
