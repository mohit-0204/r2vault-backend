package com.mxverse.storage.r2vault.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * User entity representing a registered system user.
 * Stores credentials used for JWT authentication and file path isolation.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;
}
