package com.mxverse.storage.r2vault.repository.auth;

import com.mxverse.storage.r2vault.entity.RefreshToken;
import com.mxverse.storage.r2vault.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for {@link RefreshToken} entities.
 * <p>
 * Manages the persistence and lifecycle of user refresh tokens,
 * enabling the security layer to validate and rotate session tokens.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByUser(User user);

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    int deleteByUser(User user);
}
