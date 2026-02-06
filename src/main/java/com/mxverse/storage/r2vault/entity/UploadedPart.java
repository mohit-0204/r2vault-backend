package com.mxverse.storage.r2vault.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entity representing an individual part of a multipart upload.
 * Stores the S3 ETag and size of the part for re-assembly during completion.
 */
@Entity
@Table(name = "uploaded_parts", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"session_id", "part_number"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadedPart {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private UploadSession session;

    @Column(name = "part_number", nullable = false)
    private Integer partNumber;

    @Column(name = "etag", nullable = false)
    private String etag;

    @Column(name = "size", nullable = false)
    private Long size;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
