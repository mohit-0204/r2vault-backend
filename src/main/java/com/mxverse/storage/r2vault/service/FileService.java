package com.mxverse.storage.r2vault.service;

import com.mxverse.storage.r2vault.exception.FileAccessException;
import com.mxverse.storage.r2vault.exception.FileStorageException;
import com.mxverse.storage.r2vault.exception.InvalidFileException;
import com.mxverse.storage.r2vault.model.FileRecord;
import com.mxverse.storage.r2vault.model.User;
import com.mxverse.storage.r2vault.repository.FileRecordRepository;
import com.mxverse.storage.r2vault.repository.UserRepository;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import com.mxverse.storage.r2vault.dto.FileDownloadResponse;
import com.mxverse.storage.r2vault.dto.FileMetadata;
import com.mxverse.storage.r2vault.exception.QuotaExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Service responsible for managing file operations with Cloudflare R2 storage.
 * Handles uploads, downloads, listing, and deletions while enforcing
 * user-specific quotas.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final S3Client s3Client;
    private final FileRecordRepository fileRecordRepository;
    private final UserRepository userRepository;

    @Value("${r2.bucket}")
    private String bucketName;

    /**
     * Maximum storage allowance per user (10 GB).
     */
    private static final long QUOTA_LIMIT = 10L * 1024 * 1024 * 1024;

    /**
     * Map to track the size of files currently being uploaded by each user.
     * This ensures thread-safe quota validation for concurrent uploads.
     */
    private final Map<String, AtomicLong> ongoingUploads = new ConcurrentHashMap<>();

    /**
     * Uploads a file to R2 storage for a specific user.
     * Validates remaining quota, including concurrently active uploads, before
     * proceeding.
     *
     * @param userId       The unique identifier of the user (extracted from
     *                     security
     *                     context).
     * @param file         The multipart file to be uploaded.
     * @param encryptedKey The base64 encoded encrypted AES key (optional).
     * @param iv           The base64 encoded IV (optional).
     * @return The unique S3 key assigned to the uploaded file.
     * @throws QuotaExceededException If the upload caused the user to exceed
     *                                their 10GB limit.
     * @throws FileStorageException   If there is an error reading the file stream
     *                                or uploading.
     */
    @Transactional
    public String uploadFile(String userId, MultipartFile file, String encryptedKey, String iv) {

        if (file.isEmpty()) {
            throw new InvalidFileException("File is empty");
        }
        long fileSize = file.getSize();
        validateQuota(userId, fileSize);

        // Track ongoing upload size to prevent race conditions during quota checks
        ongoingUploads.computeIfAbsent(userId, k -> new AtomicLong(0)).addAndGet(fileSize);

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.lastIndexOf(".") != -1) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // Standard isolation pattern: users/{userId}/{uuid}{extension}
            String key = "users/" + userId + "/" + UUID.randomUUID() + extension;

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .metadata(Map.of("original-filename", originalFilename != null ? originalFilename : "unknown"))
                    .build();

            log.info("Uploading file to R2: bucket={}, key={}, size={}", bucketName, key, fileSize);
            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), fileSize));

            // Persist metadata in DB
            User user = userRepository.findByUsername(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            FileRecord record = FileRecord.builder()
                    .user(user)
                    .s3Key(key)
                    .originalFilename(originalFilename != null ? originalFilename : "unknown")
                    .size(fileSize)
                    .contentType(file.getContentType())
                    .encryptedKey(encryptedKey)
                    .iv(iv)
                    .algorithm("AES/GCM/NoPadding")
                    .build();

            fileRecordRepository.save(record);

            return key;

        } catch (IOException e) {
            log.error("Failed to upload file for user {}: {}", userId, e.getMessage());
            throw new FileStorageException("Failed to read upload file stream", e);
        } finally {
            // Decrement ongoing progress regardless of success/failure
            ongoingUploads.get(userId).addAndGet(-fileSize);
        }
    }

    /**
     * Validates if a user has sufficient quota for an incoming file.
     *
     * @param userId       The user ID to check.
     * @param incomingSize The size of the file to be uploaded.
     */
    private void validateQuota(String userId, long incomingSize) {
        long currentUsage = getStorageUsage(userId);
        long currentlyUploading = ongoingUploads.getOrDefault(userId, new AtomicLong(0)).get();
        long totalProjected = currentUsage + currentlyUploading + incomingSize;

        log.debug("Quota check for user {}: current={}, uploading={}, incoming={}, projected={}, limit={}",
                userId, currentUsage, currentlyUploading, incomingSize, totalProjected, QUOTA_LIMIT);

        if (totalProjected > QUOTA_LIMIT) {
            throw new QuotaExceededException(String.format(
                    "Storage quota exceeded. Used: %d bytes, Ongoing: %d bytes, New: %d bytes, Limit: %d bytes",
                    currentUsage, currentlyUploading, incomingSize, QUOTA_LIMIT));
        }
    }

    /**
     * Retrieves a file from R2 storage with its metadata.
     * Validates that the file belongs to the requested user.
     *
     * @param key    The unique identifier of the file.
     * @param userId The user ID requesting the file.
     * @return A FileDownloadResponse containing the stream and metadata.
     */
    public FileDownloadResponse downloadFile(String key, String userId) {
        validateOwnership(key, userId);
        log.info("Downloading file from R2: bucket={}, key={}", bucketName, key);

        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        ResponseInputStream<GetObjectResponse> s3Response = s3Client.getObject(getRequest);
        GetObjectResponse metadata = s3Response.response();

        String originalFilename = metadata.metadata().get("original-filename");
        if (originalFilename == null) {
            originalFilename = key.substring(key.lastIndexOf("/") + 1);
        }

        return new FileDownloadResponse(
                s3Response,
                originalFilename,
                metadata.contentType(),
                metadata.contentLength());
    }

    /**
     * Validates that the given key starts with the expected user prefix.
     *
     * @param key    The file key.
     * @param userId The user ID.
     * @throws FileAccessException If the file does not belong to the user.
     */
    private void validateOwnership(String key, String userId) {
        if (!key.startsWith("users/" + userId + "/")) {
            log.warn("Access denied: User {} attempted to access key {}", userId, key);
            throw new FileAccessException("Access denied: You do not own this file");
        }
    }

    /**
     * Lists and filters files belonging to a specific user.
     *
     * @param userId The user ID whose files should be listed.
     * @param type   Optional content-type filter (e.g., "image", "pdf").
     * @param sortBy The field to sort by: "size", "name", or "date" (default).
     * @return A list of FileMetadata containing file details.
     */
    public List<FileMetadata> listUserFiles(String userId, String type, String sortBy) {
        String prefix = "users/" + userId + "/";
        log.info("Listing files for user: {} with prefix: {}", userId, prefix);

        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .build();

        ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);

        // Fetch all user file records from DB for metadata enrichment
        User user = userRepository.findByUsername(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Map<String, FileRecord> dbRecords = fileRecordRepository.findAllByUser(user).stream()
                .collect(Collectors.toMap(FileRecord::getS3Key, r -> r));

        List<FileMetadata> files = listResponse.contents().stream()
                .map(s3Object -> {
                    FileRecord dbRecord = dbRecords.get(s3Object.key());

                    String filename;
                    String contentType;
                    String encryptedKey = null;
                    String iv = null;
                    String algorithm = null;

                    if (dbRecord != null) {
                        filename = dbRecord.getOriginalFilename();
                        contentType = dbRecord.getContentType();
                        encryptedKey = dbRecord.getEncryptedKey();
                        iv = dbRecord.getIv();
                        algorithm = dbRecord.getAlgorithm();
                    } else {
                        // Fallback to S3 metadata for legacy/unmanaged files
                        HeadObjectRequest headRequest = HeadObjectRequest.builder()
                                .bucket(bucketName)
                                .key(s3Object.key())
                                .build();
                        HeadObjectResponse headResponse = s3Client.headObject(headRequest);
                        filename = headResponse.metadata().get("original-filename");
                        if (filename == null) {
                            filename = s3Object.key().substring(s3Object.key().lastIndexOf("/") + 1);
                        }
                        contentType = headResponse.contentType();
                    }

                    return FileMetadata.builder()
                            .key(s3Object.key())
                            .filename(filename)
                            .size(s3Object.size())
                            .contentType(contentType)
                            .lastModified(s3Object.lastModified())
                            .encryptedKey(encryptedKey)
                            .iv(iv)
                            .algorithm(algorithm)
                            .build();
                })

                .filter(file -> type == null || (file.contentType() != null && file.contentType().contains(type)))
                .collect(Collectors.toList());

        // Sort results
        if ("size".equalsIgnoreCase(sortBy)) {
            files.sort(Comparator.comparingLong(FileMetadata::size));
        } else if ("name".equalsIgnoreCase(sortBy)) {
            files.sort(Comparator.comparing(FileMetadata::filename));
        } else {
            files.sort(Comparator.comparing(FileMetadata::lastModified).reversed());
        }

        return files;
    }

    /**
     * Performs a batch deletion of multiple files from R2.
     * Filters the input list to only includes keys owned by the user.
     *
     * @param keys   A list of S3 keys to be deleted.
     * @param userId The user ID requesting the deletion.
     */
    public void deleteFiles(List<String> keys, String userId) {
        List<String> userKeys = keys.stream()
                .filter(key -> key.startsWith("users/" + userId + "/"))
                .toList();

        if (userKeys.isEmpty()) {
            log.info("No files to delete for user: {}", userId);
            return;
        }

        log.info("Deleting files for user {}: {}", userId, userKeys);

        List<ObjectIdentifier> identifiers = userKeys.stream()
                .map(key -> ObjectIdentifier.builder().key(key).build())
                .collect(Collectors.toList());

        DeleteObjectsRequest deleteRequest = DeleteObjectsRequest.builder()
                .bucket(bucketName)
                .delete(Delete.builder().objects(identifiers).build())
                .build();

        s3Client.deleteObjects(deleteRequest);

        // Batch delete from DB as well
        userKeys.forEach(fileRecordRepository::deleteByS3Key);
    }

    /**
     * Calculates the total storage consumed by a user across all their files.
     *
     * @param userId The user ID.
     * @return Total used bytes.
     */
    public long getStorageUsage(String userId) {
        String prefix = "users/" + userId + "/";
        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .build();

        ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);
        return listResponse.contents().stream()
                .mapToLong(S3Object::size)
                .sum();
    }

    /**
     * Retrieves storage usage data in a format suitable for the API response.
     *
     * @param userId The user ID.
     * @return A Map containing storage statistics.
     */
    public Map<String, Object> getUsageData(String userId) {
        long usedBytes = getStorageUsage(userId);
        return Map.of(
                "userId", userId,
                "usedBytes", usedBytes,
                "quotaBytes", QUOTA_LIMIT,
                "usagePercentage", (double) usedBytes / QUOTA_LIMIT * 100);
    }
}
