package com.mxverse.storage.r2vault.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a multipart upload session.
 * Tracks the progress of an upload, linking a {@link FileRecord} with
 * an external S3/R2 {@code uploadId}.
 */
@Entity
@Table(name = "upload_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_record_id", nullable = false)
    private FileRecord fileRecord;

    @Column(name = "upload_id", nullable = false)
    private String uploadId; // S3/R2 Upload ID

    @Column(name = "part_size", nullable = false)
    private Long partSize;

    @Column(name = "total_parts", nullable = false)
    private Integer totalParts;

    @Column(name = "total_size", nullable = false)
    private Long totalSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UploadStatus status;

    @Version
    private Integer version;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UploadedPart> uploadedParts = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (expiresAt == null) {
            expiresAt = createdAt.plusSeconds(7 * 24 * 60 * 60); // 7 days
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
