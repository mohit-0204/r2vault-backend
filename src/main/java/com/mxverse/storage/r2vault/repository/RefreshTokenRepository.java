package com.mxverse.storage.r2vault.repository;

import com.mxverse.storage.r2vault.model.RefreshToken;
import com.mxverse.storage.r2vault.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByUser(User user);

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    int deleteByUser(User user);
}
