package com.mxverse.storage.r2vault.service.multipart;

import com.mxverse.storage.r2vault.entity.UploadSession;
import com.mxverse.storage.r2vault.entity.UploadStatus;
import com.mxverse.storage.r2vault.repository.multipart.UploadSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Background service for cleaning up abandoned or expired multipart upload sessions.
 * <p>
 * Helps maintain storage hygiene and accurate quota records by identifying
 * and aborting sessions that have exceeded their time-to-live.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UploadCleanupService {

    private final UploadSessionRepository uploadSessionRepository;
    private final UploadAbortService uploadAbortService;

    /**
     * Runs every hour to clean up expired upload sessions.
     * R2 keeps multipart uploads for 7 days by default.
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void cleanupExpiredSessions() {
        log.info("Starting cleanup of expired upload sessions...");

        List<UploadSession> expiredSessions = uploadSessionRepository
                .findAllByStatusAndExpiresAtBefore(UploadStatus.INITIATED, Instant.now());

        expiredSessions.addAll(uploadSessionRepository
                .findAllByStatusAndExpiresAtBefore(UploadStatus.IN_PROGRESS, Instant.now()));

        log.info("Found {} expired sessions to cleanup", expiredSessions.size());

        for (UploadSession session : expiredSessions) {
            try {
                // We reuse the abort logic which handles R2 and DB status
                uploadAbortService.abortUpload(session.getUser().getUsername(), session.getId());
                log.info("Successfully cleaned up expired session: {}", session.getId());
            } catch (Exception e) {
                log.error("Failed to cleanup expired session {}: {}", session.getId(), e.getMessage());
            }
        }
    }
}
