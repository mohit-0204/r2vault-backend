package com.mxverse.storage.r2vault.repository.file;

import com.mxverse.storage.r2vault.entity.FileRecord;
import com.mxverse.storage.r2vault.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link FileRecord} entities.
 * <p>
 * Manages the persistence of file metadata and provides methods to look up
 * files by their unique R2 storage keys.
 */
@Repository
public interface FileRecordRepository extends JpaRepository<FileRecord, String> {
    Optional<FileRecord> findByS3Key(String s3Key);

    List<FileRecord> findAllByUser(User user);

    void deleteByS3Key(String s3Key);
}
