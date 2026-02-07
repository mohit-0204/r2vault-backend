package com.mxverse.storage.r2vault.dto.multipart;

/**
 * Data Transfer Object representing an uploaded part of a file.
 * <p>
 * This DTO is used to report the status of individual chunks in a
 * multipart upload session.
 *
 * @param partNumber The 1-based index of the part.
 * @param size       The size of the part in bytes.
 * @param etag       The ETag of the uploaded part.
 * @param createdAt  The timestamp when the part was successfully uploaded.
 */
public record UploadedPartDto(
        int partNumber,
        long size,
        String etag,
        java.time.Instant createdAt
) {
}
