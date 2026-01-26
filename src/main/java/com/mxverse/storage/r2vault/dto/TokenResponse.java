package com.mxverse.storage.r2vault.dto;

import lombok.Builder;

@Builder
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String type,
        AccountKeyMetadataDto accountKey) {

    public TokenResponse(String accessToken, String refreshToken) {
        this(accessToken, refreshToken, "Bearer", null);
    }
}
