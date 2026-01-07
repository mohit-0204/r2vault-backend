package com.mxverse.storage.r2vault.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Cloudflare R2 storage.
 * Properties are bound from the "r2" prefix in application.yml.
 */
@Configuration
@ConfigurationProperties(prefix = "r2")
@Getter
@Setter
public class R2Config {

    private String endpoint;
    private String bucket;
    private String accessKey;
    private String secretKey;

    /**
     * Creates and configures the AmazonS3 client bean for R2.
     * Note: PathStyleAccess is required for Cloudflare R2 compatibility.
     *
     * @return A configured AmazonS3 client instance.
     */
    @Bean
    public AmazonS3 r2Client() {
        return AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, "auto"))
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
                .withPathStyleAccessEnabled(true)
                .build();
    }
}
