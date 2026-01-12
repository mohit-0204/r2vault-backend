package com.mxverse.storage.r2vault.exception;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.validation.FieldError;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom FailureAnalyzer that provides user-friendly reports for configuration
 * validation errors occurred during application startup.
 * <p>
 * It maps Spring Boot property paths to their corresponding environment
 * variable names to provide clear actionable guidance to the user.
 */
public class ConfigurationFailureAnalyzer extends AbstractFailureAnalyzer<BindException> {

    private static final Map<String, String> PROPERTY_TO_ENV_MAP = new HashMap<>();

    static {
        PROPERTY_TO_ENV_MAP.put("jwt.secret", "JWT_SECRET");
        PROPERTY_TO_ENV_MAP.put("jwt.expiration", "JWT_EXPIRATION");
        PROPERTY_TO_ENV_MAP.put("jwt.refresh-expiration", "JWT_REFRESH_EXPIRATION");
        PROPERTY_TO_ENV_MAP.put("r2.endpoint", "R2_ENDPOINT");
        PROPERTY_TO_ENV_MAP.put("r2.bucket", "R2_BUCKET");
        PROPERTY_TO_ENV_MAP.put("r2.accessKey", "R2_ACCESS_KEY");
        PROPERTY_TO_ENV_MAP.put("r2.secretKey", "R2_SECRET_KEY");
        PROPERTY_TO_ENV_MAP.put("app.datasource.url", "DB_URL");
        PROPERTY_TO_ENV_MAP.put("app.datasource.username", "DB_USERNAME");
        PROPERTY_TO_ENV_MAP.put("app.datasource.password", "DB_PASSWORD");
    }

    @Override
    public FailureAnalysis analyze(@NonNull Throwable rootFailure, BindException cause) {
        StringBuilder description = new StringBuilder("Missing or invalid configuration detected.\n\n");

        description.append("Specifically:\n");

        if (cause
                .getCause() instanceof BindValidationException bve) {
            bve.getValidationErrors().getAllErrors().forEach(error -> {
                if (error instanceof FieldError fieldError) {
                    String fullProperty = cause.getName() + "." + fieldError.getField();
                    String envVar = PROPERTY_TO_ENV_MAP.getOrDefault(fullProperty,
                            "Unknown Environment Variable (" + fullProperty + ")");
                    description.append(String.format(" - Environment variable [%s] (mapped to %s) is problematic: %s\n",
                            envVar, fullProperty, fieldError.getDefaultMessage()));
                } else {
                    description.append(String.format(" - %s: %s\n", error.getObjectName(), error.getDefaultMessage()));
                }
            });
        } else {
            description.append("Reason: ").append(cause.getMessage()).append("\n");
        }

        String action = """
                Action Required:
                1. Open your .env file or check your system environment variables.
                2. Ensure the environment variables listed above are set correctly.""";

        return new FailureAnalysis(description.toString(), action, cause);
    }
}
