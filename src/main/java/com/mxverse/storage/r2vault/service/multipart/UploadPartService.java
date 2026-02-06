package com.mxverse.storage.r2vault.service.multipart;

import com.mxverse.storage.r2vault.entity.UploadSession;
import com.mxverse.storage.r2vault.entity.UploadStatus;
import com.mxverse.storage.r2vault.entity.UploadedPart;
import com.mxverse.storage.r2vault.exception.FileAccessException;
import com.mxverse.storage.r2vault.repository.multipart.UploadSessionRepository;
import com.mxverse.storage.r2vault.repository.multipart.UploadedPartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

/**
 * Service for uploading individual parts of a multipart upload.
 * <p>
 * Provides idempotency by checking for already uploaded parts before
 * sending data to Cloudflare R2.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UploadPartService {

    private final S3Client s3Client;
    private final UploadSessionRepository uploadSessionRepository;
    private final UploadedPartRepository uploadedPartRepository;

    @Value("${r2.bucket}")
    private String bucketName;

    public String uploadPart(String username, String sessionId, int partNumber, MultipartFile file) {
        // 1. Transactional check (ownership, status, idempotency)
        UploadSession session = validateSessionForPart(username, sessionId, partNumber);

        // If part already exists, return its etag immediately
        Optional<UploadedPart> existing = uploadedPartRepository.findBySessionAndPartNumber(session, partNumber);
        if (existing.isPresent()) {
            return existing.get().getEtag();
        }

        try {
            // 2. Non-transactional Upload to S3/R2
            UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                    .bucket(bucketName)
                    .key(session.getFileRecord().getS3Key())
                    .uploadId(session.getUploadId())
                    .partNumber(partNumber)
                    .build();

            log.info("Uploading part {} for session {} (size: {} bytes)", partNumber, sessionId, file.getSize());
            UploadPartResponse response = s3Client.uploadPart(uploadPartRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String etag = response.eTag();

            // 3. Transactional persist
            saveUploadedPart(session, partNumber, etag, file.getSize());

            return etag;

        } catch (IOException e) {
            log.error("Failed to read part {} stream for session {}: {}", partNumber, sessionId, e.getMessage());
            throw new RuntimeException("Failed to upload part", e);
        }
    }

    @Transactional
    public UploadSession validateSessionForPart(String username, String sessionId, int partNumber) {
        UploadSession session = uploadSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (!session.getUser().getUsername().equals(username)) {
            throw new FileAccessException("Access denied: You do not own this session");
        }

        if (session.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Upload session has expired");
        }

        if (session.getStatus() == UploadStatus.COMPLETED || session.getStatus() == UploadStatus.ABORTED) {
            throw new RuntimeException("Upload session is already " + session.getStatus());
        }

        if (partNumber < 1 || partNumber > session.getTotalParts()) {
            throw new IllegalArgumentException(String.format("Invalid part number: %d. Must be between 1 and %d",
                    partNumber, session.getTotalParts()));
        }

        return session;
    }

    @Transactional
    public void saveUploadedPart(UploadSession session, int partNumber, String etag, long size) {
        UploadedPart uploadedPart = UploadedPart.builder()
                .session(session)
                .partNumber(partNumber)
                .etag(etag)
                .size(size)
                .build();
        uploadedPartRepository.save(uploadedPart);

        // Update session status to IN_PROGRESS if first part
        if (session.getStatus() == UploadStatus.INITIATED) {
            session.setStatus(UploadStatus.IN_PROGRESS);
            uploadSessionRepository.save(session);
        }
    }
}
