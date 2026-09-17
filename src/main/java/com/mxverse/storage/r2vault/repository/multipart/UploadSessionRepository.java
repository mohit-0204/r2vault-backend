package com.mxverse.storage.r2vault.repository.multipart;

import com.mxverse.storage.r2vault.entity.UploadSession;
import com.mxverse.storage.r2vault.entity.UploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link UploadSession} entities, tracking the state of active multipart uploads.
 */
@Repository
public interface UploadSessionRepository extends JpaRepository<UploadSession, String> {
    Optional<UploadSession> findByUploadId(String uploadId);

    List<UploadSession> findAllByStatusAndExpiresAtBefore(UploadStatus status, Instant now);

    /** Fetches expired sessions with {@code user} and {@code fileRecord} eagerly loaded to avoid lazy-init issues during cleanup. */
    @Query("""
            SELECT s FROM UploadSession s
            JOIN FETCH s.user
            JOIN FETCH s.fileRecord
            WHERE s.status IN :statuses
            AND s.expiresAt < :now
            """)
    List<UploadSession> findExpiredSessionsWithAssociations(
            @Param("statuses") List<UploadStatus> statuses,
            @Param("now") Instant now
    );
}
