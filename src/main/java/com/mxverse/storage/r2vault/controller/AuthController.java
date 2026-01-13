package com.mxverse.storage.r2vault.controller;

import com.mxverse.storage.r2vault.dto.*;
import com.mxverse.storage.r2vault.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

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
     *
     * @param request The login request containing credentials.
     * @return ResponseEntity containing TokenResponse (AccessToken + RefreshToken).
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = authService.login(request.username(), request.password());
        return ResponseEntity
                .ok(ApiResponse.success(
                        response,
                        "Login successful",
                        HttpStatus.OK.value()));
    }

    /**
     * Refreshes the access token using a valid refresh token.
     *
     * @param request The refresh token request.
     * @return New access token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        TokenResponse response = authService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully", HttpStatus.OK.value()));
    }

    /**
     * Logs out the user by deleting their refresh token.
     *
     * @param principal The authenticated user (extracted from current access
     *                  token).
     * @return Success message.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(Principal principal) {
        authService.logout(principal.getName());
        return ResponseEntity
                .ok(ApiResponse.success("Log out successful!", "Log out successful!", HttpStatus.OK.value()));
    }
}
