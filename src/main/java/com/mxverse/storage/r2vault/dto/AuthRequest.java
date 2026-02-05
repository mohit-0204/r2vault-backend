package com.mxverse.storage.r2vault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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
