package com.mxverse.storage.r2vault.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration class for JWT security settings.
 * Properties are bound from the "jwt" prefix in application.yml.
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Validated
@Getter
@Setter
public class JwtConfig {

    /**
     * Secret key used to sign JWT tokens.
     */
    @NotBlank(message = "JWT secret must not be blank (set JWT_SECRET environment variable)")
    private String secret;

    /**
     * Lifetime of the access token in milliseconds.
     */
    @Min(value = 60000, message = "JWT expiration must be at least 1 minute (60000 ms)")
    private long expiration;

    /**
     * Lifetime of the refresh token in milliseconds.
     */
    @Min(value = 60000, message = "JWT refresh expiration must be at least 1 minute (60000 ms)")
    private long refreshExpiration;
}
