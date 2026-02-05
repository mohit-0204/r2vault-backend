package com.mxverse.storage.r2vault.dto;

/**
 * Data Transfer Object containing the cryptographic metadata for a user's account key.
 * Used during login and recovery to allow the client to derive keys and decrypt the master AK.
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
