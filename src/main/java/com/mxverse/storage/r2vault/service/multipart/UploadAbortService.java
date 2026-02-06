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
 * Service for aborting an ongoing multipart upload session.
 * <p>
 * It notifies Cloudflare R2 to discard uploaded parts and cleans up
 * local session state and reserved quota.
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

        // 1. Cleanup DB state first (Race protection)
        session.setStatus(UploadStatus.ABORTED);
        uploadSessionRepository.save(session);

        // Release ongoing quota reservation
        fileService.decrementOngoingUpload(username, session.getTotalSize());

        // 2. Abort in R2 (outside of main TX if possible, but here it's still in @Transactional)
        // Note: S3 abort is a cleanup operation, so we do it after committing the status if we want to be safe.
        // However, if R2 fail, we still want the status to be ABORTED.

        AbortMultipartUploadRequest abortRequest = AbortMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(session.getFileRecord().getS3Key())
                .uploadId(session.getUploadId())
                .build();

        log.info("Aborting multipart upload for session {} in R2", sessionId);
        try {
            s3Client.abortMultipartUpload(abortRequest);
        } catch (Exception e) {
            log.warn("Failed to abort multipart upload in R2 for session {}: {}", sessionId, e.getMessage());
        }
    }
}
