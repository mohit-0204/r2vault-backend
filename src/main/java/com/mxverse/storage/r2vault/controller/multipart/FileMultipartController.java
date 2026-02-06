package com.mxverse.storage.r2vault.controller.multipart;

import com.mxverse.storage.r2vault.dto.ApiResponse;
import com.mxverse.storage.r2vault.dto.multipart.InitiateUploadRequest;
import com.mxverse.storage.r2vault.dto.multipart.UploadSessionResponse;
import com.mxverse.storage.r2vault.dto.multipart.UploadStatusResponse;
import com.mxverse.storage.r2vault.service.multipart.UploadAbortService;
import com.mxverse.storage.r2vault.service.multipart.UploadCompletionService;
import com.mxverse.storage.r2vault.service.multipart.UploadInitiationService;
import com.mxverse.storage.r2vault.service.multipart.UploadPartService;
import com.mxverse.storage.r2vault.service.multipart.UploadStatusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

/**
 * Controller for handling multipart file uploads.
 * Provides endpoints for initiating, uploading parts, checking status,
 * completing, and aborting multipart upload sessions.
 * <p>
 * Relationship:
 * - Uses {@link UploadInitiationService} to start a session.
 * - Uses {@link UploadPartService} to handle individual chunks.
 * - Uses {@link UploadStatusService} to track progress.
 * - Uses {@link UploadCompletionService} and {@link UploadAbortService} for lifecycle management.
 */
@RestController
@RequestMapping("/api/files/upload")
@RequiredArgsConstructor
@Validated
@Slf4j
public class FileMultipartController {

    private final UploadInitiationService initiationService;
    private final UploadPartService partService;
    private final UploadStatusService statusService;
    private final UploadCompletionService completionService;
    private final UploadAbortService abortService;

    /**
     * Initiates a new multipart upload session.
     *
     * @param request   The initiation details (filename, size, etc.)
     * @param principal The authenticated user
     * @return UploadSessionResponse containing the R2 uploadId and backend sessionId
     */
    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<UploadSessionResponse>> initiate(
            @Valid @RequestBody InitiateUploadRequest request,
            Principal principal) {
        UploadSessionResponse response = initiationService.initiateUpload(principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Multipart upload initiated", HttpStatus.OK.value()));
    }

    /**
     * Uploads a single part of a multipart upload.
     *
     * @param sessionId  The backend session ID
     * @param partNumber The part number (1-based)
     * @param file       The file chunk
     * @param principal  The authenticated user
     * @return The ETag of the uploaded part
     */
    @PostMapping("/part")
    public ResponseEntity<ApiResponse<String>> uploadPart(
            @RequestParam("sessionId") String sessionId,
            @RequestParam("partNumber") int partNumber,
            @RequestParam("file") MultipartFile file,
            Principal principal) {
        String etag = partService.uploadPart(principal.getName(), sessionId, partNumber, file);
        return ResponseEntity.ok(ApiResponse.success(etag, "Part " + partNumber + " uploaded", HttpStatus.OK.value()));
    }

    /**
     * Retrieves the status of a multipart upload session, including already uploaded parts.
     * Useful for resuming interrupted uploads.
     *
     * @param sessionId The backend session ID
     * @param principal The authenticated user
     * @return UploadStatusResponse containing session state and uploaded parts
     */
    @GetMapping("/status/{sessionId}")
    public ResponseEntity<ApiResponse<UploadStatusResponse>> getStatus(
            @PathVariable String sessionId,
            Principal principal) {
        UploadStatusResponse response = statusService.getUploadStatus(principal.getName(), sessionId);
        return ResponseEntity.ok(ApiResponse.success(response, "Upload status retrieved", HttpStatus.OK.value()));
    }

    /**
     * Completes a multipart upload once all parts are uploaded.
     *
     * @param sessionId The backend session ID
     * @param principal The authenticated user
     * @return Structured response indicating success
     */
    @PostMapping("/complete/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> complete(
            @PathVariable String sessionId,
            Principal principal) {
        completionService.completeUpload(principal.getName(), sessionId);
        return ResponseEntity.ok(ApiResponse.success(null, "Upload completed successfully", HttpStatus.OK.value()));
    }

    /**
     * Aborts a multipart upload and cleans up resources.
     *
     * @param sessionId The backend session ID
     * @param principal The authenticated user
     * @return Structured response indicating success
     */
    @PostMapping("/abort/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> abort(
            @PathVariable String sessionId,
            Principal principal) {
        abortService.abortUpload(principal.getName(), sessionId);
        return ResponseEntity.ok(ApiResponse.success(null, "Upload aborted", HttpStatus.OK.value()));
    }
}
