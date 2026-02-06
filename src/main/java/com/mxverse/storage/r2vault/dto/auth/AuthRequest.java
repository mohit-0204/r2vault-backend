package com.mxverse.storage.r2vault.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for authentication requests.
 * <p>
 * Used for both registration and login. It carries credentials,
 * device information, and cryptographic keys required for zero-knowledge storage.
 *
 * @param username           The user's unique identification name.
 * @param password           The user's plaintext password (encoded on the server).
 * @param deviceId           Unique identifier for the logging device.
 * @param deviceName         Human-readable name for the device.
 * @param wrappedAccountKey  The user's Master Key wrapped with their UMK.
 * @param recoveryWrappedKey The Master Key wrapped for recovery.
 * @param kdfSalt            Salt used for Key Derivation Function.
 * @param kdfIterations      Number of iterations for Argon2/PBKDF2.
 * @param evictDeviceId      Optional ID of a device to evict if limit is reached.
 */
public record AuthRequest(
        @NotBlank(message = "Username is required") @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters") String username,

        @NotBlank(message = "Password is required") @Size(min = 6, message = "Password must be at least 6 characters") String password,

        String deviceId,
        String deviceName,
        String wrappedAccountKey,
        String recoveryWrappedKey,
        String kdfSalt,
        Integer kdfIterations,
        String evictDeviceId) {
}
