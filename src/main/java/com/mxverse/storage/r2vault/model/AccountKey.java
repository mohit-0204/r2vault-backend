package com.mxverse.storage.r2vault.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "account_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "wrapped_key", nullable = false, columnDefinition = "TEXT")
    private String wrappedKey; // AK wrapped with UMK (Base64)

    @Column(name = "kdf_salt", nullable = false)
    private String kdfSalt; // Base64

    @Column(name = "kdf_iterations", nullable = false)
    private Integer kdfIterations;
}
