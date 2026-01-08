package com.mxverse.storage.r2vault.controller;

import com.mxverse.storage.r2vault.dto.FileMetadata;
import com.mxverse.storage.r2vault.service.FileService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.List;

import com.mxverse.storage.r2vault.dto.ApiResponse;

import java.util.Map;

/**
 * REST Controller for managing user file operations.
 * All endpoints in this controller are protected and requires a valid JWT
 * token.
 * User identity is automatically derived from the security context (Principal).
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Validated
public class FileController {

    private final FileService fileService;

    /**
     * Uploads a file for the currently authenticated user.
     *
     * @param file      The multipart file to upload.
     * @param principal The authenticated user principal.
     * @return ResponseEntity with the generated file key or error message.
     * @throws IOException If file processing fails.
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<String>> upload(
            @RequestParam("file") MultipartFile file,
            Principal principal) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File is empty", 400));
        }

        String key = fileService.uploadFile(principal.getName(), file);
        return ResponseEntity.ok(ApiResponse.success(key, "File uploaded successfully", 200));
    }

    /**
     * Downloads a file belonging to the authenticated user.
     * Validates that the requested key starts with the user's specific prefix.
     *
     * @param key       The S3 key of the file to download.
     * @param principal The authenticated user principal.
     * @return ResponseEntity containing the file stream as an attachment.
     */
    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> download(@RequestParam @NotBlank String key, Principal principal) {
        // Path guarding: ensure the user can only access files in their isolated
        // directory
        if (!key.startsWith("users/" + principal.getName() + "/")) {
            return ResponseEntity.status(403).build();
        }

        InputStream inputStream = fileService.downloadFile(key);
        InputStreamResource resource = new InputStreamResource(inputStream);

        String filename = key.substring(key.lastIndexOf("/") + 1);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    /**
     * Lists files for the authenticated user with optional filtering and sorting.
     *
     * @param principal The authenticated user principal.
     * @param type      Optional content-type filter (e.g., "image").
     * @param sortBy    Field to sort by: "size", "name", or "date" (default).
     * @return List of file metadata objects.
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<FileMetadata>>> listFiles(
            Principal principal,
            @RequestParam(required = false) String type,
            @RequestParam(required = false, defaultValue = "date") String sortBy) {
        List<FileMetadata> files = fileService.listUserFiles(principal.getName(), type, sortBy);
        return ResponseEntity.ok(ApiResponse.success(files, "Files retrieved successfully", 200));
    }

    /**
     * Batch deletes a list of files.
     * Automatically filters the input list to only include keys belonging to the
     * authenticated user.
     *
     * @param keys      List of S3 keys to delete.
     * @param principal The authenticated user principal.
     * @return 204 No Content response.
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteFiles(@RequestBody List<String> keys, Principal principal) {
        // Enforce user isolation: only allow deletion of owned files
        List<String> userKeys = keys.stream()
                .filter(key -> key.startsWith("users/" + principal.getName() + "/"))
                .toList();

        if (!userKeys.isEmpty()) {
            fileService.deleteFiles(userKeys);
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Files deleted successfully", 200));
    }

    /**
     * Retrieves the storage usage statistics for the authenticated user.
     *
     * @param principal The authenticated user principal.
     * @return Map containing usedBytes, quotaBytes, and usagePercentage.
     */
    @GetMapping("/usage")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUsage(Principal principal) {
        String userId = principal.getName();
        long usedBytes = fileService.getStorageUsage(userId);
        long quotaBytes = fileService.getQuotaLimit();

        Map<String, Object> usageData = Map.of(
                "userId", userId,
                "usedBytes", usedBytes,
                "quotaBytes", quotaBytes,
                "usagePercentage", (double) usedBytes / quotaBytes * 100);

        return ResponseEntity.ok(ApiResponse.success(usageData, "Usage statistics retrieved", 200));
    }
}
