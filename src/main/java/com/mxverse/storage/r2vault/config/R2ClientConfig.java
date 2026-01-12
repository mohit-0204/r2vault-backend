package com.mxverse.storage.r2vault.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the AWS S3 (Cloudflare R2) client bean.
 * Uses {@link R2Properties} for its settings.
 */
@Configuration
@RequiredArgsConstructor
public class R2ClientConfig {

    private final R2Properties r2Properties;

    /**
     * Creates and configures the AmazonS3 client bean for R2 storage.
     *
     * @return A configured AmazonS3 client instance.
     */
    @Bean
    public AmazonS3 r2Client() {
        if (r2Properties.getAccessKey() == null || r2Properties.getSecretKey() == null ||
                r2Properties.getAccessKey().isBlank() || r2Properties.getSecretKey().isBlank()) {
            // Return null if credentials are missing to allow @Validated validation to
            // handle errors gracefully
            return null;
        }
        return AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(r2Properties.getEndpoint(), "auto"))
                .withCredentials(new AWSStaticCredentialsProvider(
                        new BasicAWSCredentials(r2Properties.getAccessKey(), r2Properties.getSecretKey())))
                .build();
    }
}
