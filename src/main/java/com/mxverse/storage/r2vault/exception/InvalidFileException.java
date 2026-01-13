package com.mxverse.storage.r2vault.exception;

/**
 * Thrown when a file provided for upload is invalid (e.g., empty or corrupt).
 * Maps to 400 Bad Request.
 */
public class InvalidFileException extends RuntimeException {
    public InvalidFileException(String message) {
        super(message);
    }
}
