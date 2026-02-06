package com.mxverse.storage.r2vault.repository.auth;

import com.mxverse.storage.r2vault.entity.AccountKey;
import com.mxverse.storage.r2vault.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for {@link AccountKey} entities.
 * <p>
 * Manages the persistence of user-specific cryptographic keys and
 * metadata required for zero-knowledge file encryption.
 */
@Repository
public interface AccountKeyRepository extends JpaRepository<AccountKey, Long> {
    Optional<AccountKey> findByUser(User user);

    Optional<AccountKey> findByUserId(Long userId);
}
