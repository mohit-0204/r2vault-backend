package com.mxverse.storage.r2vault.repository.auth;

import com.mxverse.storage.r2vault.entity.Device;
import com.mxverse.storage.r2vault.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link Device} entities.
 * <p>
 * Handles the storage and retrieval of logged-in device metadata,
 * supporting device limit enforcement and session auditing.
 */
@Repository
public interface DeviceRepository extends JpaRepository<Device, String> {
    List<Device> findByUserAndIsActive(User user, boolean isActive);

    Optional<Device> findByUserAndDeviceId(User user, String deviceId);

    List<Device> findByUserIdOrderByLastActiveAtAsc(Long userId);
}
