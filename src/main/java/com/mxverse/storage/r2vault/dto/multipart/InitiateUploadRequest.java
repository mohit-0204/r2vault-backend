package com.mxverse.storage.r2vault.dto.multipart;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request DTO to initiate a multipart upload.
 * <p>
 * Contains the necessary metadata to start an upload session with the server
 * and Cloudflare R2.
 *
 * @param fileName     Original name of the file to be uploaded.
 * @param totalSize    Total size of the file in bytes.
 * @param contentType  MIME type of the file.
 * @param encryptedKey Optional Base64 encoded encrypted file key for zero-knowledge.
 * @param iv           Optional Base64 encoded initialization vector.
 */
public record InitiateUploadRequest(
        @NotBlank String fileName,
        @NotNull @Positive Long totalSize,
        @NotBlank String contentType,
        String encryptedKey,
        String iv,
        Integer totalParts
) {
}
