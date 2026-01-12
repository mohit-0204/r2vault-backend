package com.mxverse.storage.r2vault.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the application datasource.
 * These are bound from the "app.datasource" prefix.
 */
@Configuration
@ConfigurationProperties(prefix = "app.datasource")
@Validated
@Getter
@Setter
public class DataSourceProperties {

    /**
     * JDBC URL for the database connection.
     */
    @NotBlank(message = "Database URL must not be blank (set DB_URL environment variable)")
    private String url;

    /**
     * Username for the database connection.
     */
    @NotBlank(message = "Database username must not be blank (set DB_USERNAME environment variable)")
    private String username;

    /**
     * Password for the database connection.
     */
    @NotBlank(message = "Database password must not be blank (set DB_PASSWORD environment variable)")
    private String password;

    /**
     * Driver class name for the database connection.
     */
    private String driverClassName;
}
