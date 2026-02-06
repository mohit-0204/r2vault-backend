package com.mxverse.storage.r2vault.repository.auth;

import com.mxverse.storage.r2vault.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository interface for {@link User} entities.
 * <p>
 * Provides standard CRUD operations and custom query methods for managing
 * user accounts and authentication.
 */
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
