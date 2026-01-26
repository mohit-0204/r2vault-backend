package com.mxverse.storage.r2vault.exception;

import com.mxverse.storage.r2vault.model.Device;
import lombok.Getter;
import java.util.List;

@Getter
public class DeviceLimitExceededException extends RuntimeException {
    private final List<Device> activeDevices;

    public DeviceLimitExceededException(String message, List<Device> activeDevices) {
        super(message);
        this.activeDevices = activeDevices;
    }
}
