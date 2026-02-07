package com.mxverse.storage.r2vault.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Entity representing a file stored in the R2 bucket.
 * Contains metadata, encryption details (IV, encrypted key),
 * and ownership information.
 */
@Entity
@Table(name = "file_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "s3_key", nullable = false, unique = true)
    private String s3Key;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "size", nullable = false)
    private Long size;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "encrypted_key", columnDefinition = "TEXT")
    private String encryptedKey; // Base64 encoded

    @Column(name = "iv", columnDefinition = "TEXT")
    private String iv; // Base64 encoded

    @Column(name = "algorithm")
    private String algorithm;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "fileRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UploadSession> uploadSessions;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
