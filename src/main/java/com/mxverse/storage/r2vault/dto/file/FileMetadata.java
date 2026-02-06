package com.mxverse.storage.r2vault.dto.file;

import lombok.Builder;

import java.time.Instant;

/**
 * Data Transfer Object representing file information retrieved from R2 storage.
 * <p>
 * Provides comprehensive metadata including encryption details, size, and
 * timestamps, allowing clients to identify and decrypt files.
 *
 * @param key          The unique R2 object key.
 * @param filename     The original name of the file.
 * @param size         File size in bytes.
 * @param contentType  MIME type of the file.
 * @param lastModified Timestamp of the last modification.
 * @param encryptedKey Base64 encoded encrypted file key.
 * @param iv           Base64 encoded initialization vector.
 * @param algorithm    Encryption algorithm used (e.g., "AES/GCM/NoPadding").
 */
@Builder
public record FileMetadata(
        String key,
        String filename,
        long size,
        String contentType,
        Instant lastModified,
        String encryptedKey,
        String iv,
        String algorithm) {
}
