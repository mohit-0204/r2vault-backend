package com.mxverse.storage.r2vault.service;

import com.mxverse.storage.r2vault.dto.multipart.InitiateUploadRequest;
import com.mxverse.storage.r2vault.dto.multipart.UploadSessionResponse;
import com.mxverse.storage.r2vault.dto.multipart.UploadStatusResponse;
import com.mxverse.storage.r2vault.service.multipart.*;
import com.mxverse.storage.r2vault.entity.UploadSession;
import com.mxverse.storage.r2vault.entity.UploadStatus;
import com.mxverse.storage.r2vault.entity.User;
import com.mxverse.storage.r2vault.repository.multipart.UploadSessionRepository;
import com.mxverse.storage.r2vault.repository.auth.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
@org.springframework.test.context.ActiveProfiles("test")
public class MultipartUploadIntegrationTest {

    @Autowired
    private UploadInitiationService initiationService;

    @Autowired
    private UploadPartService partService;

    @Autowired
    private UploadStatusService statusService;

    @Autowired
    private UploadCompletionService completionService;

    @Autowired
    private UploadAbortService abortService;

    @Autowired
    private UploadCleanupService cleanupService;

    @Autowired
    private UploadSessionRepository sessionRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private S3Client s3Client;

    private final String USERNAME = "testuser";
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username(USERNAME)
                .password("password")
                .build();
        userRepository.save(testUser);

        // Mock S3 responses
        when(s3Client.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
                .thenReturn(CreateMultipartUploadResponse.builder().uploadId("s3-upload-id").build());
        when(s3Client.uploadPart(any(UploadPartRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
                .thenReturn(UploadPartResponse.builder().eTag("etag-1").build());
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(ListObjectsV2Response.builder().contents(List.of()).isTruncated(false).build());
    }

    @Test
    void test1_HappyPath() {
        // 1. Initiate
        InitiateUploadRequest request = new InitiateUploadRequest("test.bin", 15 * 1024 * 1024L, "application/octet-stream", null, null, null);
        UploadSessionResponse initiateResponse = initiationService.initiateUpload(USERNAME, request);
        assertNotNull(initiateResponse.uploadId());
        String sessionId = initiateResponse.sessionId();

        // 2. Upload 3 parts (5MB each)
        MockMultipartFile partFile = new MockMultipartFile("file", "test.bin", "application/octet-stream", new byte[5 * 1024 * 1024]);

        partService.uploadPart(USERNAME, sessionId, 1, partFile);
        partService.uploadPart(USERNAME, sessionId, 2, partFile);
        partService.uploadPart(USERNAME, sessionId, 3, partFile);

        // 3. Complete
        completionService.completeUpload(USERNAME, sessionId);

        UploadSession session = sessionRepository.findById(sessionId).orElseThrow();
        assertEquals(UploadStatus.COMPLETED, session.getStatus());
        verify(s3Client, times(1)).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
    }

    @Test
    void test2_IdempotentPartUpload() {
        InitiateUploadRequest request = new InitiateUploadRequest("test.bin", 10 * 1024 * 1024L, "application/octet-stream", null, null, null);
        UploadSessionResponse initiateResponse = initiationService.initiateUpload(USERNAME, request);
        String sessionId = initiateResponse.sessionId();

        MockMultipartFile partFile = new MockMultipartFile("file", "test.bin", "application/octet-stream", new byte[5 * 1024 * 1024]);

        // Upload same part twice
        String etag1 = partService.uploadPart(USERNAME, sessionId, 1, partFile);
        String etag2 = partService.uploadPart(USERNAME, sessionId, 1, partFile);

        assertEquals(etag1, etag2);
        // S3 should only be called once for the same part (our logic check before call)
        verify(s3Client, times(1)).uploadPart(any(UploadPartRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class));
    }

    @Test
    void test3_ResumeFlow() {
        InitiateUploadRequest request = new InitiateUploadRequest("test.bin", 10 * 1024 * 1024L, "application/octet-stream", null, null, null);
        UploadSessionResponse initiateResponse = initiationService.initiateUpload(USERNAME, request);
        String sessionId = initiateResponse.sessionId();

        MockMultipartFile partFile = new MockMultipartFile("file", "test.bin", "application/octet-stream", new byte[5 * 1024 * 1024]);
        partService.uploadPart(USERNAME, sessionId, 1, partFile);

        // Get status to resume
        UploadStatusResponse status = statusService.getUploadStatus(USERNAME, sessionId);
        assertEquals(1, status.uploadedParts().size());
        assertEquals(1, status.uploadedParts().getFirst().partNumber());

        // Resume: Upload part 2
        partService.uploadPart(USERNAME, sessionId, 2, partFile);
        completionService.completeUpload(USERNAME, sessionId);

        UploadSession session = sessionRepository.findById(sessionId).orElseThrow();
        assertEquals(UploadStatus.COMPLETED, session.getStatus());
    }

    @Test
    void test4_AbortRace() {
        InitiateUploadRequest request = new InitiateUploadRequest("test.bin", 10 * 1024 * 1024L, "application/octet-stream", null, null, null);
        UploadSessionResponse initiateResponse = initiationService.initiateUpload(USERNAME, request);
        String sessionId = initiateResponse.sessionId();

        // Abort
        abortService.abortUpload(USERNAME, sessionId);

        UploadSession session = sessionRepository.findById(sessionId).orElseThrow();
        assertEquals(UploadStatus.ABORTED, session.getStatus());

        // Try to upload part after abort
        MockMultipartFile partFile = new MockMultipartFile("file", "test.bin", "application/octet-stream", new byte[5 * 1024 * 1024]);
        assertThrows(RuntimeException.class, () -> partService.uploadPart(USERNAME, sessionId, 1, partFile));
    }

    @Test
    void test5_ExpiryCleanup() {
        InitiateUploadRequest request = new InitiateUploadRequest("test.bin", 10 * 1024 * 1024L, "application/octet-stream", null, null, null);
        UploadSessionResponse initiateResponse = initiationService.initiateUpload(USERNAME, request);
        String sessionId = initiateResponse.sessionId();

        // Force expiry in past
        UploadSession session = sessionRepository.findById(sessionId).orElseThrow();
        session.setExpiresAt(Instant.now().minusSeconds(3600));
        sessionRepository.save(session);

        // Run cleanup
        cleanupService.cleanupExpiredSessions();

        UploadSession updatedSession = sessionRepository.findById(sessionId).orElseThrow();
        assertEquals(UploadStatus.ABORTED, updatedSession.getStatus());
        verify(s3Client, times(1)).abortMultipartUpload(any(AbortMultipartUploadRequest.class));
    }
}
