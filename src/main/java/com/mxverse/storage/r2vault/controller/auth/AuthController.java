package com.mxverse.storage.r2vault.controller.auth;

import com.mxverse.storage.r2vault.dto.ApiResponse;
import com.mxverse.storage.r2vault.dto.auth.AccountKeyMetadataDto;
import com.mxverse.storage.r2vault.dto.auth.AuthRequest;
import com.mxverse.storage.r2vault.dto.auth.DeviceDto;
import com.mxverse.storage.r2vault.dto.auth.ResetPasswordRequest;
import com.mxverse.storage.r2vault.dto.auth.TokenRefreshRequest;
import com.mxverse.storage.r2vault.dto.auth.TokenResponse;
import com.mxverse.storage.r2vault.exception.DeviceLimitExceededException;
import com.mxverse.storage.r2vault.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for authentication and authorization.
 * Handles user registration, login, logout, and token refreshing.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * Registers a new user in the system.
     * <p>
     * Creates a new user record and optionally registers the first device
     * and recovery metadata.
     *
     * @param request The registration request containing username and password.
     * @return ResponseEntity with success message or error if username exists.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody AuthRequest request) {
        authService.registerUser(request);
        return ResponseEntity
                .ok(ApiResponse.success(
                        "User registered successfully",
                        "User registered successfully",
                        HttpStatus.OK.value()));
    }

    /**
     * Authenticates a user and generates both Access and Refresh tokens.
     * <p>
     * Performs credential verification and manages device login/eviction logic.
     *
     * @param request The login request containing credentials.
     * @return ResponseEntity containing TokenResponse (AccessToken + RefreshToken).
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody AuthRequest request) {
        TokenResponse response = authService.login(request);
        return ResponseEntity
                .ok(ApiResponse.success(
                        response,
                        "Login successful",
                        HttpStatus.OK.value()));
    }

    /**
     * Refreshes the access token using a valid refresh token.
     * <p>
     * Extends the session without requiring the user to re-enter credentials.
     *
     * @param request The refresh token request.
     * @return New access token wrapped in a TokenResponse.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request) {
        TokenResponse response = authService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(
                ApiResponse.success(response, "Token refreshed successfully", HttpStatus.OK.value()));
    }

    /**
     * Logs out the user by deleting their refresh token.
     * <p>
     * Prevents future token refreshes for the current session.
     *
     * @param principal The authenticated user (extracted from current access token).
     * @return Success message.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(Principal principal) {
        authService.logout(principal.getName());
        return ResponseEntity
                .ok(ApiResponse.success("Log out successful!", "Log out successful!",
                        HttpStatus.OK.value()));
    }

    /**
     * Verifies the integrity of the current session.
     * <p>
     * Used by clients at startup to verify that the local session has not been
     * revoked. If the session is invalid, the security filter will return a 403,
     * triggering a local data wipe for enhanced security.
     *
     * @param principal The authenticated user principal.
     * @return ApiResponse confirming session validity.
     */
    @GetMapping("/session")
    public ResponseEntity<ApiResponse<String>> checkSession(Principal principal) {
        return ResponseEntity.ok(ApiResponse.success("Session valid", "Session valid", HttpStatus.OK.value()));
    }

    /**
     * Retrieves the cryptographic metadata required for account recovery.
     * <p>
     * This metadata is essential for the client to perform key derivation during recovery.
     *
     * @param username The username of the user seeking recovery.
     * @return ResponseEntity containing the recovery metadata.
     */
    @GetMapping("/recovery-metadata")
    public ResponseEntity<ApiResponse<AccountKeyMetadataDto>> getRecoveryMetadata(@RequestParam String username) {
        AccountKeyMetadataDto metadata = authService.getRecoveryMetadata(username);
        return ResponseEntity.ok(ApiResponse.success(metadata, "Recovery metadata retrieved", HttpStatus.OK.value()));
    }

    /**
     * Completes the password reset process.
     * <p>
     * Requires the new password and the account key re-wrapped with the new UMK.
     * This is the final step in the account recovery flow.
     *
     * @param request The reset password request payload.
     * @return ResponseEntity confirming the reset.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successful", "Password reset successful", HttpStatus.OK.value()));
    }

    /**
     * Special handler for DeviceLimitExceededException.
     * <p>
     * Returns a 403 Forbidden status along with the list of currently active
     * devices to facilitate user-initiated device eviction.
     *
     * @param ex The exception caught.
     * @return ResponseEntity with 403 status and device list.
     */
    @ExceptionHandler(DeviceLimitExceededException.class)
    public ResponseEntity<ApiResponse<List<DeviceDto>>> handleDeviceLimit(DeviceLimitExceededException ex) {
        List<DeviceDto> devices = ex.getActiveDevices().stream()
                .map(d -> new DeviceDto(d.getId(), d.getName(), d.getPlatform(),
                        d.getLastActiveAt().toString()))
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(
                        devices,
                        "Device limit exceeded",
                        403));
    }
}
