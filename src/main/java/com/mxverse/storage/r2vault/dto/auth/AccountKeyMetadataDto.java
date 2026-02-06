package com.mxverse.storage.r2vault.dto.auth;

/**
 * Data Transfer Object containing the cryptographic metadata for a user's account key.
 * <p>
 * Used during login and recovery to allow the client to derive keys and decrypt
 * the master Account Key (AK). This is crucial for maintaining zero-knowledge
 * storage where the server never sees the plaintext master key.
 *
 * @param wrappedKey         The AK wrapped with the User Master Key.
 * @param recoveryWrappedKey The AK wrapped with the Recovery Key (if available).
 * @param salt               The Base64 encoded salt used for UMK derivation.
 * @param iterations         The number of KDF iterations required.
 */
public record AccountKeyMetadataDto(
        String wrappedKey,
        String recoveryWrappedKey,
        String salt,
        Integer iterations) {
}
