package com.mxverse.storage.r2vault.repository.multipart;

import com.mxverse.storage.r2vault.entity.UploadedPart;
import com.mxverse.storage.r2vault.entity.UploadSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link UploadedPart} entities.
 * <p>
 * Persists the ETag and part number for each chunk of a multipart upload,
 * ensuring that progress is saved correctly and parts are ordered for completion.
 */
@Repository
public interface UploadedPartRepository extends JpaRepository<UploadedPart, String> {
    Optional<UploadedPart> findBySessionAndPartNumber(UploadSession session, Integer partNumber);

    List<UploadedPart> findAllBySessionOrderByPartNumberAsc(UploadSession session);
}
