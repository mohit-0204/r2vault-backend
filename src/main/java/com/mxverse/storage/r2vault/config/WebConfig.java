package com.mxverse.storage.r2vault.config;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * Global CORS configuration for the API.
 * <p>
 * Secure by Default:
 * If 'CORS_ALLOWED_ORIGINS' is not configured or empty, CORS is disabled.
 * This is the most secure configuration for backends only serving mobile apps or internal tools.
 * <p>
 * Flexibility:
 * Supports '*' for development and specific domains for production.
 * Logs warnings to guide developers when configuration is missing or insecure.
 */
@Slf4j
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        if (allowedOrigins == null || allowedOrigins.length == 0 || (allowedOrigins.length == 1 && allowedOrigins[0].isEmpty())) {
            log.warn("CORS_ALLOWED_ORIGINS is not configured or empty. CORS is DISABLED.");
            log.info("Browser-based frontends will be blocked. This is normal if you only use the Mobile App or Postman.");
            return;
        }

        if (Arrays.asList(allowedOrigins).contains("*")) {
            log.warn("INSECURE CONFIGURATION: CORS is allowed for ALL origins (*). Use this for development only!");
        } else {
            log.info("CORS configured with allowed origins: {}", Arrays.toString(allowedOrigins));
        }

        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .exposedHeaders("Authorization", "Content-Disposition");
    }
}
