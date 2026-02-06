package com.mxverse.storage.r2vault.dto.multipart;

/**
 * Response DTO returning session details for a started multipart upload.
 * <p>
 * Provides the identifiers and configuration required for the client to
 * slice and upload file parts.
 *
 * @param uploadId   The external S3/R2 upload identifier.
 * @param sessionId  The internal backend session identifier.
 * @param partSize   The fixed part size (in bytes) expected by the server.
 * @param totalParts The total number of parts the client must upload.
 */
public record UploadSessionResponse(
        String uploadId,
        String sessionId,
        long partSize,
        int totalParts
) {
}
