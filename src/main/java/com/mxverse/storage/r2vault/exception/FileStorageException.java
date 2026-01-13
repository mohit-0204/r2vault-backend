package com.mxverse.storage.r2vault.exception;

/**
 * Thrown when an unexpected error occurs during file storage operations (e.g.,
 * IO failures).
 * Maps to 500 Internal Server Error.
 */
public class FileStorageException extends RuntimeException {
    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
