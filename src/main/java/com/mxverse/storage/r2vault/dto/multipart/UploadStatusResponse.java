package com.mxverse.storage.r2vault.dto.multipart;

import com.mxverse.storage.r2vault.entity.UploadStatus;

import java.util.List;

/**
 * Response DTO providing the current status of a multipart upload.
 * <p>
 * This is used for tracking progress and facilitating resume operations by
 * listing all parts that have already been successfully uploaded.
 *
 * @param uploadId      The external S3/R2 upload identifier.
 * @param sessionId     The internal backend session identifier.
 * @param status        The current state of the upload (e.g., IN_PROGRESS, COMPLETED).
 * @param totalParts    The total number of parts expected for this file.
 * @param totalSize     The total file size in bytes.
 * @param uploadedParts List of metadata for parts that have been uploaded.
 */
public record UploadStatusResponse(
        String uploadId,
        String sessionId,
        UploadStatus status,
        int totalParts,
        long totalSize,
        List<UploadedPartDto> uploadedParts
) {
}
