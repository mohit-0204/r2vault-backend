package com.mxverse.storage.r2vault.dto;

public record DeviceDto(
        String id,
        String name,
        String platform,
        String lastActive) {
}
