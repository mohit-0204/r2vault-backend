package com.mxverse.storage.r2vault.dto;

public record AccountKeyMetadataDto(
        String wrappedKey,
        String salt,
        Integer iterations) {
}
