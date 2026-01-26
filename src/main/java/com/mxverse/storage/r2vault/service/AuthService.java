package com.mxverse.storage.r2vault.service;

import com.mxverse.storage.r2vault.dto.AccountKeyMetadataDto;
import com.mxverse.storage.r2vault.dto.AuthRequest;
import com.mxverse.storage.r2vault.dto.TokenResponse;
import com.mxverse.storage.r2vault.exception.DeviceLimitExceededException;
import com.mxverse.storage.r2vault.exception.TokenRefreshException;
import com.mxverse.storage.r2vault.exception.UserAlreadyExistsException;
import com.mxverse.storage.r2vault.model.AccountKey;
import com.mxverse.storage.r2vault.model.Device;
import com.mxverse.storage.r2vault.model.RefreshToken;
import com.mxverse.storage.r2vault.model.User;
import com.mxverse.storage.r2vault.repository.AccountKeyRepository;
import com.mxverse.storage.r2vault.repository.DeviceRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

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
                .map(ak -> new AccountKeyMetadataDto(ak.getWrappedKey(), ak.getKdfSalt(), ak.getKdfIterations()))
                .orElse(null);

        log.info("User logged in: {}", user.getUsername());
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .type("Bearer")
                .accountKey(akDto)
                .build();
    }

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
}
