package com.mxverse.storage.r2vault.service.multipart;

import com.mxverse.storage.r2vault.dto.multipart.UploadStatusResponse;
import com.mxverse.storage.r2vault.dto.multipart.UploadedPartDto;
import com.mxverse.storage.r2vault.entity.UploadSession;
import com.mxverse.storage.r2vault.exception.FileAccessException;
import com.mxverse.storage.r2vault.repository.multipart.UploadSessionRepository;
import com.mxverse.storage.r2vault.repository.multipart.UploadedPartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for retrieving the current status of a multipart upload session.
 * <p>
 * Used by clients to resume interrupted uploads by identifying
 * which parts have already been successfully uploaded.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UploadStatusService {

    private final UploadSessionRepository uploadSessionRepository;
    private final UploadedPartRepository uploadedPartRepository;

    @Transactional(readOnly = true)
    public UploadStatusResponse getUploadStatus(String username, String sessionId) {
        UploadSession session = uploadSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (!session.getUser().getUsername().equals(username)) {
            throw new FileAccessException("Access denied: You do not own this session");
        }

        if (session.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Upload session has expired");
        }

        List<UploadedPartDto> uploadedParts = uploadedPartRepository.findAllBySessionOrderByPartNumberAsc(session)
                .stream()
                .map(part -> new UploadedPartDto(part.getPartNumber(), part.getSize(), part.getCreatedAt()))
                .collect(Collectors.toList());

        return new UploadStatusResponse(
                session.getUploadId(),
                session.getId(),
                session.getStatus(),
                session.getTotalParts(),
                session.getTotalSize(),
                uploadedParts
        );
    }
}
