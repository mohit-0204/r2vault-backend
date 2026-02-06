package com.mxverse.storage.r2vault.controller.file;

import com.mxverse.storage.r2vault.dto.ApiResponse;
import com.mxverse.storage.r2vault.dto.file.FileDownloadResponse;
import com.mxverse.storage.r2vault.dto.file.FileMetadata;
import com.mxverse.storage.r2vault.service.file.FileService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for managing user file operations.
 * <p>
 * All endpoints in this controller are protected and require a valid JWT
 * token. User identity is automatically derived from the security context (Principal),
 * ensuring that users can only manage their own files.
 * <p>
 * Relationship:
 * - Uses {@link FileService} for business logic and storage interaction.
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Validated
public class FileController {

    private final FileService fileService;

    /**
     * Uploads a file for the currently authenticated user.
     * <p>
     * This method handles smaller file uploads. For large files,
     * use the multipart upload endpoints.
     *
     * @param file         The multipart file to upload.
     * @param encryptedKey Optional Base64 encoded encrypted key for client-side encryption.
     * @param iv           Optional Base64 encoded initialization vector.
     * @param principal    The authenticated user principal.
     * @return ResponseEntity with the generated file key or error message.
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "encryptedKey", required = false) String encryptedKey,
            @RequestParam(value = "iv", required = false) String iv,
            Principal principal) {

        String key = fileService.uploadFile(principal.getName(), file, encryptedKey, iv);

        return ResponseEntity.ok(ApiResponse.success(key, "File uploaded successfully", HttpStatus.OK.value()));
    }

    /**
     * Downloads a file belonging to the authenticated user.
     * <p>
     * Streams the file content directly from the storage provider.
     *
     * @param key       The S3 key of the file to download.
     * @param principal The authenticated user principal.
     * @return ResponseEntity containing the file stream as an attachment.
     */
    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> download(@RequestParam @NotBlank String key, Principal principal) {
        FileDownloadResponse response = fileService.downloadFile(key, principal.getName());
        InputStreamResource resource = new InputStreamResource(response.inputStream());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + response.fileName() + "\"")
                .contentType(MediaType.parseMediaType(response.contentType()))
                .contentLength(response.size())
                .body(resource);
    }

    /**
     * Lists files for the authenticated user with optional filtering and sorting.
     * <p>
     * Provides metadata for all files owned by the user.
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
        return ResponseEntity.ok(ApiResponse.success(files, "Files retrieved successfully", HttpStatus.OK.value()));
    }

    /**
     * Batch deletes a list of files.
     *
     * @param keys      List of S3 keys to delete.
     * @param principal The authenticated user principal.
     * @return Success response.
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteFiles(@RequestBody List<String> keys, Principal principal) {
        fileService.deleteFiles(keys, principal.getName());
        return ResponseEntity.ok(ApiResponse.success(null, "Files deleted successfully", HttpStatus.OK.value()));
    }

    /**
     * Retrieves the storage usage statistics for the authenticated user.
     *
     * @param principal The authenticated user principal.
     * @return Map containing usedBytes, quotaBytes, and usagePercentage.
     */
    @GetMapping("/usage")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStorageUsage(Principal principal) {
        Map<String, Object> usageData = fileService.getUsageData(principal.getName());
        return ResponseEntity.ok(ApiResponse.success(usageData, "Usage statistics retrieved", HttpStatus.OK.value()));
    }
}
