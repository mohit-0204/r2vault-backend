package com.mxverse.storage.r2vault.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entity representing a user's logged-in device.
 * <p>
 * Tracks device-specific metadata and activity per user. This is crucial
 * for enforcing device limits and providing security auditing.
 *
 * @see User
 */
@Entity
@Table(name = "devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "device_id", nullable = false)
    private String deviceId; // Client-provided unique ID

    @Column(name = "name")
    private String name;

    @Column(name = "platform")
    private String platform;

    @Column(name = "last_active_at", nullable = false)
    private Instant lastActiveAt;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

}
