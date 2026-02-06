package com.mxverse.storage.r2vault.dto.auth;

/**
 * Data Transfer Object representing a user's logged-in device.
 * <p>
 * This DTO is primarily used during device eviction flows to help users
 * identify and select which device to disconnect.
 *
 * @param id         Unique internal ID of the device.
 * @param name       User-friendly name of the device (e.g., "iPhone 13").
 * @param platform   Operating system or platform of the device.
 * @param lastActive ISO-8601 timestamp of the last activity.
 */
public record DeviceDto(
        String id,
        String name,
        String platform,
        String lastActive) {
}
