package com.mxverse.storage.r2vault.service;

import com.mxverse.storage.r2vault.dto.AuthRequest;
import com.mxverse.storage.r2vault.dto.TokenResponse;
import com.mxverse.storage.r2vault.exception.TokenRefreshException;
import com.mxverse.storage.r2vault.exception.UserAlreadyExistsException;
import com.mxverse.storage.r2vault.model.RefreshToken;
import com.mxverse.storage.r2vault.model.User;
import com.mxverse.storage.r2vault.repository.UserRepository;
import com.mxverse.storage.r2vault.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;

    /**
     * Registers a new user with an encoded password.
     *
     * @param request The registration request containing username and password.
     * @throws UserAlreadyExistsException if the username is already taken.
     */
    public void registerUser(AuthRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new UserAlreadyExistsException("User already exists");
        }

        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .build();
        userRepository.save(user);
        log.info("Successfully registered user: {}", request.username());
    }

    /**
     * Authenticates a user and generates both access and refresh tokens.
     *
     * @param username The username of the user.
     * @param password The raw password.
     * @return A TokenResponse containing the JWT access token and refresh token.
     */
    public TokenResponse login(String username, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = jwtUtils.generateToken(authentication.getName());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(authentication.getName());

        log.info("User logged in: {}", username);
        return new TokenResponse(accessToken, refreshToken.getToken());
    }

    /**
     * Logs out the user by deleting their refresh token from the database.
     *
     * @param username The username of the user to log out.
     */
    public void logout(String username) {
        refreshTokenService.deleteByUserId(username);
        log.info("User logged out: {}", username);
    }

    /**
     * Refreshes the access token using a valid, non-expired refresh token.
     *
     * @param refreshToken The refresh token string.
     * @return A new TokenResponse with a fresh access token.
     * @throws TokenRefreshException if the token is invalid or expired.
     */
    public TokenResponse refreshToken(String refreshToken) {
        return refreshTokenService.findByToken(refreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    log.info("Access token refreshed for user: {}", user.getUsername());
                    String token = jwtUtils.generateToken(user.getUsername());
                    return new TokenResponse(token, refreshToken);
                })
                .orElseThrow(() -> {
                    log.error("Refresh token not found: {}", refreshToken);
                    return new TokenRefreshException(refreshToken, "Refresh token is not in database!");
                });
    }
}
