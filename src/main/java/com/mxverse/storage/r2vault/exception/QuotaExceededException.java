package com.mxverse.storage.r2vault.exception;

/**
 * Exception thrown when a user's upload would exceed their allocated storage quota.
 * <p>
 * This check is performed before initiating an upload to prevent
 * over-utilization of resources.
 */
public class QuotaExceededException extends RuntimeException {
    public QuotaExceededException(String message) {
        super(message);
    }
}
