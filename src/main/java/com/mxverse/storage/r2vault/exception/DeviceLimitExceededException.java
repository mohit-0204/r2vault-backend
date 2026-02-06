package com.mxverse.storage.r2vault.exception;

import com.mxverse.storage.r2vault.entity.Device;
import lombok.Getter;

import java.util.List;

/**
 * Exception thrown when a user attempts to log in from more devices than allowed.
 * <p>
 * This exception carries the list of currently active devices to allow
 * the client to present a device eviction UI to the user.
 */
@Getter
public class DeviceLimitExceededException extends RuntimeException {
    private final List<Device> activeDevices;

    public DeviceLimitExceededException(String message, List<Device> activeDevices) {
        super(message);
        this.activeDevices = activeDevices;
    }
}
