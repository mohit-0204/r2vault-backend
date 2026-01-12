package com.mxverse.storage.r2vault.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Cloudflare R2 storage.
 * Bound from the "r2" prefix in application.yml.
 */
@Configuration
@ConfigurationProperties(prefix = "r2")
@Validated
@Getter
@Setter
public class R2Properties {

    @NotBlank(message = "R2 endpoint must not be blank (set R2_ENDPOINT environment variable)")
    private String endpoint;

    @NotBlank(message = "R2 bucket name must not be blank (set R2_BUCKET environment variable)")
    private String bucket;

    @NotBlank(message = "R2 access key must not be blank (set R2_ACCESS_KEY environment variable)")
    private String accessKey;

    @NotBlank(message = "R2 secret key must not be blank (set R2_SECRET_KEY environment variable)")
    private String secretKey;
}
