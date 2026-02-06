package com.mxverse.storage.r2vault.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Cloudflare R2 storage.
 * <p>
 * These properties are bound from the {@code r2} prefix in the application configuration.
 * They define the credentials and endpoint required to communicate with R2 via the AWS S3 SDK.
 */
@Configuration
@ConfigurationProperties(prefix = "r2")
@Validated
@Getter
@Setter
public class R2Properties {

    /**
     * The public or private endpoint for the R2 bucket.
     */
    @NotBlank(message = "R2 endpoint must not be blank (set R2_ENDPOINT environment variable)")
    private String endpoint;

    /**
     * The name of the R2 bucket to use.
     */
    @NotBlank(message = "R2 bucket name must not be blank (set R2_BUCKET environment variable)")
    private String bucket;

    /**
     * The access key (ID) for R2 authentication.
     */
    @NotBlank(message = "R2 access key must not be blank (set R2_ACCESS_KEY environment variable)")
    private String accessKey;

    /**
     * The secret key for R2 authentication.
     */
    @NotBlank(message = "R2 secret key must not be blank (set R2_SECRET_KEY environment variable)")
    private String secretKey;
}
