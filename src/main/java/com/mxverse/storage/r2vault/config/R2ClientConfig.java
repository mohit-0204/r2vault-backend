package com.mxverse.storage.r2vault.config;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

/**
 * Configuration for the AWS S3 (Cloudflare R2) client bean.
 * Uses {@link R2Properties} for its settings.
 */
@Configuration
@RequiredArgsConstructor
public class R2ClientConfig {

    private final R2Properties r2Properties;

    /**
     * Creates and configures the S3Client bean for R2 storage.
     *
     * @return A configured S3Client instance.
     */
    @Bean
    public S3Client s3Client() {
        if (r2Properties.getAccessKey() == null || r2Properties.getSecretKey() == null ||
                r2Properties.getAccessKey().isBlank() || r2Properties.getSecretKey().isBlank()) {
            // Return null if credentials are missing to allow @Validated validation to
            // handle errors gracefully
            return null;
        }

        return S3Client.builder()
                .endpointOverride(URI.create(r2Properties.getEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(r2Properties.getAccessKey(),
                                r2Properties.getSecretKey())))
                .region(Region.of("auto")) // R2 uses 'auto' or specific regions
                .build();
    }
}
