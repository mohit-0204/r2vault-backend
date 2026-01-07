package com.mxverse.storage.r2vault.service;

import com.mxverse.storage.r2vault.exception.TokenRefreshException;
import com.mxverse.storage.r2vault.model.RefreshToken;
import com.mxverse.storage.r2vault.model.User;
import com.mxverse.storage.r2vault.repository.RefreshTokenRepository;
import com.mxverse.storage.r2vault.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Service to manage the lifecycle of refresh tokens.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${jwt.refresh-expiration}")
    private long refreshTokenExpirationMs;

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    /**
     * Creates a new refresh token for a user or updates the existing one.
     */
    @Transactional
    public RefreshToken createRefreshToken(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // If user already has a token, we update it instead of deleting to avoid
        // unique constraint issues in the same transaction
        RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
                .orElse(new RefreshToken());

        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenExpirationMs));

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Validates if the token has expired.
     */
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(),
                    "Refresh token was expired. Please make a new signin request");
        }
        return token;
    }

    /**
     * Revokes a token (Logout).
     */
    @Transactional
    public int deleteByUserId(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return refreshTokenRepository.deleteByUser(user);
    }
}
