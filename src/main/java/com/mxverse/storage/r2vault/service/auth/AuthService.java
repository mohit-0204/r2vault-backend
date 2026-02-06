package com.mxverse.storage.r2vault.service.auth;

import com.mxverse.storage.r2vault.dto.auth.AccountKeyMetadataDto;
import com.mxverse.storage.r2vault.dto.auth.AuthRequest;
import com.mxverse.storage.r2vault.dto.auth.ResetPasswordRequest;
import com.mxverse.storage.r2vault.dto.auth.TokenResponse;
import com.mxverse.storage.r2vault.exception.DeviceLimitExceededException;
import com.mxverse.storage.r2vault.exception.ResourceNotFoundException;
import com.mxverse.storage.r2vault.exception.TokenRefreshException;
import com.mxverse.storage.r2vault.exception.UserAlreadyExistsException;
import com.mxverse.storage.r2vault.entity.AccountKey;
import com.mxverse.storage.r2vault.entity.Device;
import com.mxverse.storage.r2vault.entity.RefreshToken;
import com.mxverse.storage.r2vault.entity.User;
import com.mxverse.storage.r2vault.repository.auth.AccountKeyRepository;
import com.mxverse.storage.r2vault.repository.auth.DeviceRepository;
import com.mxverse.storage.r2vault.repository.auth.UserRepository;
import com.mxverse.storage.r2vault.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service managing user authentication, registration, and account recovery.
 * <p>
 * This service orchestrates the security flow, including password hashing,
 * device-aware login limits, and zero-knowledge account key storage.
 * It interfaces with {@link JwtUtils} and {@link RefreshTokenService}
 * to handle session management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final AccountKeyRepository accountKeyRepository;
    private final DeviceRepository deviceRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;

    private static final int MAX_DEVICES = 2;

    /**
     * Registers a new user with an encoded password.
     *
     * @param request The registration request containing username and password.
     * @throws UserAlreadyExistsException if the username is already taken.
     */
    @Transactional
    public void registerUser(AuthRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new UserAlreadyExistsException("User already exists");
        }

        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .build();
        user = userRepository.save(user);

        // Store Account Key if provided (First device registration)
        if (request.wrappedAccountKey() != null) {
            AccountKey accountKey = AccountKey.builder()
                    .user(user)
                    .wrappedKey(request.wrappedAccountKey())
                    .recoveryWrappedKey(request.recoveryWrappedKey())
                    .kdfSalt(request.kdfSalt())
                    .kdfIterations(request.kdfIterations())
                    .build();
            accountKeyRepository.save(accountKey);
        }

        // Register First Device
        if (request.deviceId() != null) {
            registerDevice(user, request.deviceId(), request.deviceName());
        }

        log.info("Successfully registered user: {}", request.username());
    }

    /**
     * Authenticates a user and generates both access and refresh tokens.
     *
     * @param request the AuthRequest obj containing username and pass
     * @return A TokenResponse containing the JWT access token and refresh token.
     */
    @Transactional
    public TokenResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new RuntimeException("User not found after auth"));

        // Device Management
        if (request.deviceId() != null) {
            // Handle explicit eviction request
            if (request.evictDeviceId() != null) {
                deviceRepository.findById(request.evictDeviceId()).ifPresent(device -> {
                    if (device.getUser().getId().equals(user.getId())) {
                        device.setActive(false);
                        deviceRepository.save(device);
                        // Also kill the session for that device if we can
                        refreshTokenService.deleteByUserId(user.getUsername());
                        log.info("Evicted device: {} for user: {}", request.evictDeviceId(), user.getUsername());
                    }
                });
            }
            handleDeviceLogin(user, request.deviceId(), request.deviceName());
        }

        String accessToken = jwtUtils.generateToken(user.getUsername());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getUsername());

        // Fetch Account Key metadata
        AccountKeyMetadataDto akDto = accountKeyRepository.findByUser(user)
                .map(ak -> new AccountKeyMetadataDto(ak.getWrappedKey(), ak.getRecoveryWrappedKey(), ak.getKdfSalt(), ak.getKdfIterations()))
                .orElse(null);

        log.info("User logged in: {}", user.getUsername());
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .type("Bearer")
                .accountKey(akDto)
                .build();
    }

    /**
     * Handles the device-specific logic during login.
     * Checks if the device is already registered or if the device limit has been reached.
     *
     * @param user     The authenticated user.
     * @param deviceId The unique ID of the device.
     * @param name     The descriptive name of the device.
     * @throws DeviceLimitExceededException if the user has reached the maximum allowed active devices.
     */
    private void handleDeviceLogin(User user, String deviceId, String name) {
        Optional<Device> existing = deviceRepository.findByUserAndDeviceId(user, deviceId);
        if (existing.isPresent()) {
            Device device = existing.get();
            device.setLastActiveAt(Instant.now());
            device.setActive(true);
            deviceRepository.save(device);
            return;
        }

        List<Device> activeDevices = deviceRepository.findByUserAndIsActive(user, true);
        if (activeDevices.size() >= MAX_DEVICES) {
            throw new DeviceLimitExceededException("Device limit exceeded", activeDevices);
        }

        registerDevice(user, deviceId, name);
    }

    /**
     * Registers a new device for a user.
     *
     * @param user     The user to whom the device belongs.
     * @param deviceId The unique hardware/app ID of the device.
     * @param name     The display name of the device.
     */
    private void registerDevice(User user, String deviceId, String name) {
        Device device = Device.builder()
                .user(user)
                .deviceId(deviceId)
                .name(name)
                .platform("Mobile") // Could be passed in request
                .lastActiveAt(Instant.now())
                .isActive(true)
                .build();
        deviceRepository.save(device);
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
                    return new TokenResponse(token, refreshToken, "Bearer", null);
                })
                .orElseThrow(() -> {
                    log.error("Refresh token not found: {}", refreshToken);
                    return new TokenRefreshException(refreshToken, "Refresh token is not in database!");
                });
    }

    /**
     * Retrieves the account recovery metadata for a given username.
     * This metadata includes the wrapped account key and KDF parameters.
     *
     * @param username The username for which to retrieve metadata.
     * @return The AccountKeyMetadataDto containing recovery info.
     * @throws ResourceNotFoundException if the user or metadata is not found.
     */
    @Transactional(readOnly = true)
    public AccountKeyMetadataDto getRecoveryMetadata(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return accountKeyRepository.findByUser(user)
                .map(ak -> new AccountKeyMetadataDto(
                        ak.getWrappedKey(),
                        ak.getRecoveryWrappedKey(),
                        ak.getKdfSalt(),
                        ak.getKdfIterations()
                ))
                .orElseThrow(() -> new ResourceNotFoundException("Recovery metadata not found"));
    }

    /**
     * Resets the user's password and updates their wrapped account key.
     * This is the final step of the recovery flow, performed after mnemonic verification.
     *
     * @param request The reset request containing the new password and re-wrapped key.
     * @throws ResourceNotFoundException if the user or the existing account key is not found.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        AccountKey accountKey = accountKeyRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Account key not found"));

        accountKey.setWrappedKey(request.wrappedAccountKey());
        accountKey.setKdfSalt(request.kdfSalt());
        accountKey.setKdfIterations(request.kdfIterations());
        accountKeyRepository.save(accountKey);

        log.info("Password and Account Key reset successfully for user: {}", request.username());
    }
}
