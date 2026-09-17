package com.mxverse.storage.r2vault.service.multipart;

import com.mxverse.storage.r2vault.service.file.FileService;

import com.mxverse.storage.r2vault.exception.FileAccessException;
import com.mxverse.storage.r2vault.entity.UploadSession;
import com.mxverse.storage.r2vault.entity.UploadStatus;
import com.mxverse.storage.r2vault.repository.multipart.UploadSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;

/**
 * Handles aborting multipart upload sessions — notifies R2 to discard
 * uploaded parts and releases the user's quota reservation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UploadAbortService {

    private final S3Client s3Client;
    private final UploadSessionRepository uploadSessionRepository;
    private final FileService fileService;

    @Value("${r2.bucket}")
    private String bucketName;

    /** User-facing abort — validates that the session belongs to {@code username} before aborting. */
    @Transactional
    public void abortUpload(String username, String sessionId) {
        UploadSession session = uploadSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (!session.getUser().getUsername().equals(username)) {
            throw new FileAccessException("Access denied: You do not own this session");
        }

        if (session.getStatus() == UploadStatus.COMPLETED || session.getStatus() == UploadStatus.ABORTED) {
            return;
        }

        doAbort(session);
    }

    /** System-facing abort — skips ownership validation; caller guarantees the session is eligible. */
    @Transactional
    public void abortSessionForCleanup(UploadSession session) {
        if (session.getStatus() == UploadStatus.COMPLETED || session.getStatus() == UploadStatus.ABORTED) {
            log.debug("Session {} is already in a terminal state ({}), skipping cleanup.",
                    session.getId(), session.getStatus());
            return;
        }
        doAbort(session);
    }

    private void doAbort(UploadSession session) {
        // Mark ABORTED before touching R2 — keeps DB consistent if the R2 call fails.
        session.setStatus(UploadStatus.ABORTED);
        uploadSessionRepository.save(session);

        fileService.decrementOngoingUpload(session.getUser().getUsername(), session.getTotalSize());

        AbortMultipartUploadRequest abortRequest = AbortMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(session.getFileRecord().getS3Key())
                .uploadId(session.getUploadId())
                .build();

        log.info("Aborting multipart upload for session {} in R2", session.getId());
        try {
            s3Client.abortMultipartUpload(abortRequest);
        } catch (Exception e) {
            log.warn("Failed to abort multipart upload in R2 for session {}: {}", session.getId(), e.getMessage());
        }
    }
}
