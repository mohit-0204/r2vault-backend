package com.mxverse.storage.r2vault.repository.multipart;

import com.mxverse.storage.r2vault.entity.UploadSession;
import com.mxverse.storage.r2vault.entity.UploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link UploadSession} entities.
 * <p>
 * Tracks the state of active multipart upload sessions, allowing the server
 * to resume or clean up expired uploads.
 */
@Repository
public interface UploadSessionRepository extends JpaRepository<UploadSession, String> {
    Optional<UploadSession> findByUploadId(String uploadId);

    List<UploadSession> findAllByStatusAndExpiresAtBefore(UploadStatus status, Instant now);
}
