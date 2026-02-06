package com.mxverse.storage.r2vault.dto.file;

import java.io.InputStream;

/**
 * Data transfer object encapsulating a file download stream and its metadata.
 */
public record FileDownloadResponse(
        InputStream inputStream,
        String fileName,
        String contentType,
        long size) {
}
