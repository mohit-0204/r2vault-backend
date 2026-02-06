package com.mxverse.storage.r2vault.service.multipart;

import com.mxverse.storage.r2vault.service.file.FileService;

import com.mxverse.storage.r2vault.dto.multipart.InitiateUploadRequest;
import com.mxverse.storage.r2vault.dto.multipart.UploadSessionResponse;
import com.mxverse.storage.r2vault.entity.FileRecord;
import com.mxverse.storage.r2vault.entity.UploadSession;
import com.mxverse.storage.r2vault.entity.UploadStatus;
import com.mxverse.storage.r2vault.entity.User;
import com.mxverse.storage.r2vault.repository.file.FileRecordRepository;
import com.mxverse.storage.r2vault.repository.multipart.UploadSessionRepository;
import com.mxverse.storage.r2vault.repository.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;

import java.util.Map;
import java.util.UUID;

/**
 * Service responsible for initiating a multipart upload session.
 * <p>
 * It reserves storage quota, generates a unique S3 key, and handshakes with Cloudflare R2
 * to create a new Multipart Upload.
 * <p>
 * Relationship:
 * - Creates {@link FileRecord} in PENDING state.
 * - Creates {@link UploadSession} to track the R2 upload lifecycle.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UploadInitiationService {

    private final S3Client s3Client;
    private final FileService fileService;
    private final FileRecordRepository fileRecordRepository;
    private final UploadSessionRepository uploadSessionRepository;
    private final UserRepository userRepository;

    @Value("${r2.bucket}")
    private String bucketName;

    private static final long MIN_PART_SIZE = 5 * 1024 * 1024; // 5MB

    @Transactional
    public UploadSessionResponse initiateUpload(String username, InitiateUploadRequest request) {
        log.info("Initiating multipart upload for user {}: {}", username, request.fileName());

        // 1. Quota reservation
        fileService.validateQuota(username, request.totalSize());
        fileService.incrementOngoingUpload(username, request.totalSize());

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Generate key
        String extension = "";
        if (request.fileName().lastIndexOf(".") != -1) {
            extension = request.fileName().substring(request.fileName().lastIndexOf("."));
        }
        String s3Key = "users/" + username + "/" + UUID.randomUUID() + extension;

        // 3. Create Multipart Upload in R2
        CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(request.contentType())
                .metadata(Map.of("original-filename", request.fileName()))
                .build();

        CreateMultipartUploadResponse createResponse = s3Client.createMultipartUpload(createRequest);
        String uploadId = createResponse.uploadId();

        // 4. Create FileRecord (Pending)
        FileRecord fileRecord = FileRecord.builder()
                .user(user)
                .s3Key(s3Key)
                .originalFilename(request.fileName())
                .size(request.totalSize())
                .contentType(request.contentType())
                .encryptedKey(request.encryptedKey())
                .iv(request.iv())
                .algorithm("AES/GCM/NoPadding")
                .build();
        fileRecord = fileRecordRepository.save(fileRecord);

        // 5. Create UploadSession
        long partSize = MIN_PART_SIZE;
        int totalParts = request.totalParts() != null ? request.totalParts() :
                (int) Math.ceil((double) request.totalSize() / partSize);

        UploadSession session = UploadSession.builder()
                .user(user)
                .fileRecord(fileRecord)
                .uploadId(uploadId)
                .partSize(partSize)
                .totalParts(totalParts)
                .totalSize(request.totalSize())
                .status(UploadStatus.INITIATED)
                .build();

        session = uploadSessionRepository.save(session);

        return new UploadSessionResponse(uploadId, session.getId(), partSize, totalParts);
    }
}
