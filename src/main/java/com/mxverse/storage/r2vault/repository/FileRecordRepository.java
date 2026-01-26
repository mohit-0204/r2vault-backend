package com.mxverse.storage.r2vault.repository;

import com.mxverse.storage.r2vault.model.FileRecord;
import com.mxverse.storage.r2vault.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRecordRepository extends JpaRepository<FileRecord, String> {
    Optional<FileRecord> findByS3Key(String s3Key);

    List<FileRecord> findAllByUser(User user);

    void deleteByS3Key(String s3Key);
}
