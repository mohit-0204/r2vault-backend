package com.mxverse.storage.r2vault.exception;

/**
 * Thrown when a user attempts to access a file they do not own.
 * Maps to 403 Forbidden.
 */
public class FileAccessException extends RuntimeException {
    public FileAccessException(String message) {
        super(message);
    }
}
