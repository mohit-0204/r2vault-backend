package com.mxverse.storage.r2vault.entity;

/**
 * Enumeration of possible states for a multipart upload session.
 * <p>
 * Tracks the lifecycle of an upload from initiation to final
 * completion or abort.
 */
public enum UploadStatus {
    INITIATED,
    IN_PROGRESS,
    COMPLETED,
    ABORTED
}
