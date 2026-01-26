package com.mxverse.storage.r2vault.repository;

import com.mxverse.storage.r2vault.model.Device;
import com.mxverse.storage.r2vault.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, String> {
    List<Device> findByUserAndIsActive(User user, boolean isActive);

    Optional<Device> findByUserAndDeviceId(User user, String deviceId);

    List<Device> findByUserIdOrderByLastActiveAtAsc(Long userId);
}
