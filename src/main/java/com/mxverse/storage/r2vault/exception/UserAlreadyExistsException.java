package com.mxverse.storage.r2vault.exception;

/**
 * Thrown when an attempt is made to register a user with a username that
 * already exists.
 * Maps to 409 Conflict.
 */
public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
