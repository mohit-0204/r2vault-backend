package com.mxverse.storage.r2vault.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing a user's master account key (AK) metadata.
 * <p>
 * The AK is used to encrypt and decrypt the user's files and is itself protected
 * by several wrapping layers (Password-derived UMK and Mnemonic-derived Recovery Key).
 * This structure ensures zero-knowledge storage.
 */
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

    /**
     * The Master Account Key, wrapped (encrypted) with the User Master Key (UMK).
     * The UMK is derived from the user's password using Argon2/PBKDF2.
     * Stored as a Base64 encoded string.
     */
    @Column(name = "wrapped_key", nullable = false, columnDefinition = "TEXT")
    private String wrappedKey;

    /**
     * The Master Account Key, wrapped (encrypted) with the Recovery Key.
     * The Recovery Key is derived from the user's BIP-39 mnemonic phrase.
     * This field is used during account recovery if the password is forgotten.
     * Stored as a Base64 encoded string.
     */
    @Column(name = "recovery_wrapped_key", columnDefinition = "TEXT")
    private String recoveryWrappedKey;

    /**
     * The salt used during Key Derivation (KDF) to generate the UMK.
     * Stored as a Base64 encoded string.
     */
    @Column(name = "kdf_salt", nullable = false)
    private String kdfSalt;

    /**
     * The number of iterations/rounds used during Key Derivation.
     */
    @Column(name = "kdf_iterations", nullable = false)
    private Integer kdfIterations;
}
