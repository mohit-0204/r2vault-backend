package com.mxverse.storage.r2vault.dto;

import lombok.Builder;
import java.time.Instant;

/**
 * Data Transfer Object representing file information retrieved from R2 storage.
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
