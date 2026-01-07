package com.mxverse.storage.r2vault.exception;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<String> handleQuotaExceededException(QuotaExceededException e) {
        return ResponseEntity.status(HttpStatus.INSUFFICIENT_STORAGE).body(e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> handleMaxSizeException(MaxUploadSizeExceededException exc) {
        return ResponseEntity.status(HttpStatus.valueOf(413)).body("File size exceeds limit!");
    }

    @ExceptionHandler(TokenRefreshException.class)
    public ResponseEntity<String> handleTokenRefreshException(TokenRefreshException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(AmazonServiceException.class)
    public ResponseEntity<String> handleS3ServiceException(AmazonServiceException e) {
        return ResponseEntity.status(e.getStatusCode()).body("S3 Service Error: " + e.getErrorMessage());
    }

    @ExceptionHandler(SdkClientException.class)
    public ResponseEntity<String> handleS3ClientException(SdkClientException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("S3 Client Error: " + e.getMessage());
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<String> handleIOException(IOException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("IO Error: " + e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error: " + e.getMessage());
    }
}
