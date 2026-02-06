package com.mxverse.storage.r2vault.service.multipart;

import com.mxverse.storage.r2vault.entity.UploadSession;
import com.mxverse.storage.r2vault.entity.UploadStatus;
import com.mxverse.storage.r2vault.entity.UploadedPart;
import com.mxverse.storage.r2vault.exception.FileAccessException;
import com.mxverse.storage.r2vault.repository.multipart.UploadSessionRepository;
import com.mxverse.storage.r2vault.repository.multipart.UploadedPartRepository;
import com.mxverse.storage.r2vault.service.file.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for finalizing a multipart upload.
 * <p>
 * It gathers all uploaded parts, sends a completion request to Cloudflare R2,
 * and updates the file status to COMPLETED.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UploadCompletionService {

    private final S3Client s3Client;
    private final UploadSessionRepository uploadSessionRepository;
    private final UploadedPartRepository uploadedPartRepository;
    private final FileService fileService;

    @Value("${r2.bucket}")
    private String bucketName;

    @Transactional
    public void completeUpload(String username, String sessionId) {
        UploadSession session = uploadSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (!session.getUser().getUsername().equals(username)) {
            throw new FileAccessException("Access denied: You do not own this session");
        }

        if (session.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Upload session has expired");
        }

        if (session.getStatus() == UploadStatus.COMPLETED) {
            log.info("Session {} already completed.", sessionId);
            return;
        }

        // 1. Validation: Contiguous parts
        List<UploadedPart> parts = uploadedPartRepository.findAllBySessionOrderByPartNumberAsc(session);
        if (parts.size() < session.getTotalParts()) {
            throw new RuntimeException("Cannot complete upload: some parts are missing. Uploaded: "
                    + parts.size() + "/" + session.getTotalParts());
        }

        // 2. Prepare R2 Complete Request
        List<CompletedPart> completedParts = parts.stream()
                .map(part -> CompletedPart.builder()
                        .partNumber(part.getPartNumber())
                        .eTag(part.getEtag())
                        .build())
                .collect(Collectors.toList());

        CompletedMultipartUpload completedMultipartUpload = CompletedMultipartUpload.builder()
                .parts(completedParts)
                .build();

        CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(session.getFileRecord().getS3Key())
                .uploadId(session.getUploadId())
                .multipartUpload(completedMultipartUpload)
                .build();

        log.info("Completing multipart upload for session {} in R2", sessionId);
        s3Client.completeMultipartUpload(completeRequest);

        // 3. Finalize DB state
        session.setStatus(UploadStatus.COMPLETED);
        uploadSessionRepository.save(session);

        // Release ongoing quota reservation
        fileService.decrementOngoingUpload(username, session.getTotalSize());

        // Note: The FileRecord will need its status updated or similar if we decide 
        // to have a dedicated status field there too. For now, we'll mark the session.
    }
}
