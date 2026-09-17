package com.mxverse.storage.r2vault.service.multipart;

import com.mxverse.storage.r2vault.entity.UploadSession;
import com.mxverse.storage.r2vault.entity.UploadStatus;
import com.mxverse.storage.r2vault.repository.multipart.UploadSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Background service that cleans up abandoned or expired multipart upload sessions,
 * maintaining storage hygiene and accurate quota records.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UploadCleanupService {

    private final UploadSessionRepository uploadSessionRepository;
    private final UploadAbortService uploadAbortService;

    /** Statuses that are still active and therefore abortable. */
    private static final List<UploadStatus> ABORTABLE_STATUSES =
            List.of(UploadStatus.INITIATED, UploadStatus.IN_PROGRESS);

    @Scheduled(fixedRate = 3600000) // 1 hour
    @Transactional
    public void cleanupExpiredSessions() {
        log.info("Starting cleanup of expired upload sessions...");

        List<UploadSession> expiredSessions =
                uploadSessionRepository.findExpiredSessionsWithAssociations(ABORTABLE_STATUSES, Instant.now());

        log.info("Found {} expired sessions to cleanup", expiredSessions.size());

        for (UploadSession session : expiredSessions) {
            try {
                uploadAbortService.abortSessionForCleanup(session);
                log.info("Successfully cleaned up expired session: {}", session.getId());
            } catch (Exception e) {
                log.error("Failed to cleanup expired session {}: {}", session.getId(), e.getMessage());
            }
        }
    }
}
