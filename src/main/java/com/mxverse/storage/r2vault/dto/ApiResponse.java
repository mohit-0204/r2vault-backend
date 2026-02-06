package com.mxverse.storage.r2vault.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Generic wrapper for all API responses.
 * <p>
 * This class provides a consistent structure for success and error messages,
 * including a status code, human-readable message, and the actual data payload.
 *
 * @param <T> The type of the data payload.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private int status;
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(T data, String message, int status) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Success", 200);
    }

    public static <T> ApiResponse<T> error(String message, int status) {
        return error(null, message, status);
    }

    public static <T> ApiResponse<T> error(T data, String message, int status) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(data)
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
