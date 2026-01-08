package com.mxverse.storage.r2vault.controller;

import com.mxverse.storage.r2vault.dto.*;
import com.mxverse.storage.r2vault.exception.TokenRefreshException;
import com.mxverse.storage.r2vault.model.RefreshToken;
import com.mxverse.storage.r2vault.model.User;
import com.mxverse.storage.r2vault.repository.UserRepository;
import com.mxverse.storage.r2vault.service.RefreshTokenService;
import com.mxverse.storage.r2vault.util.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * Controller handling user authentication and registration.
 * Provides endpoints for creating new accounts and obtaining JWT tokens.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;

    /**
     * Registers a new user in the system.
     * Passwords are encrypted using BCrypt before storage.
     *
     * @param request The registration request containing username and password.
     * @return ResponseEntity with success message or error if username exists.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody AuthRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("User already exists", 400));
        }

        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .build();
        userRepository.save(user);

        log.info("Successfully registered user: {}", request.username());
        return ResponseEntity
                .ok(ApiResponse.success("User registered successfully", "User registered successfully", 200));
    }

    /**
     * Authenticates a user and generates both Access and Refresh tokens.
     *
     * @param request The login request containing credentials.
     * @return ResponseEntity containing TokenResponse (AccessToken + RefreshToken).
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        if (userRepository.findByUsername(request.username()).isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("User not found", HttpStatus.UNAUTHORIZED.value()));
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String accessToken = jwtUtils.generateToken(authentication.getName());
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(authentication.getName());

            log.info("User logged in: {}", request.username());

            return ResponseEntity.ok(ApiResponse.success(
                    new TokenResponse(accessToken, refreshToken.getToken()),
                    "Login successful",
                    HttpStatus.OK.value()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Incorrect password", HttpStatus.UNAUTHORIZED.value()));
        }
    }

    /**
     * Refreshes the access token using a valid refresh token.
     *
     * @param request The refresh token request.
     * @return New access token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.refreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    log.info("Access token refreshed for user: {}", user.getUsername());
                    String token = jwtUtils.generateToken(user.getUsername());
                    return ResponseEntity.ok(ApiResponse.success(new TokenResponse(token, requestRefreshToken),
                            "Token refreshed successfully", 200));
                })
                .orElseThrow(() -> {
                    log.error("Refresh token not found: {}", requestRefreshToken);
                    return new TokenRefreshException(requestRefreshToken, "Refresh token is not in database!");
                });
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
        refreshTokenService.deleteByUserId(principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Log out successful!", "Log out successful!", 200));
    }
}
