package com.mxverse.storage.r2vault.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.sql.DataSource;

/**
 * Configuration properties for the primary datasource.
 * <p>
 * These properties are bound from the {@code app.datasource} prefix and are
 * used to configure the {@link DataSource} bean.
 */
@Configuration
@ConfigurationProperties(prefix = "app.datasource")
@Validated
@Getter
@Setter
public class DataSourceProperties {

    /**
     * The JDBC URL for connecting to the PostgreSQL database.
     */
    @NotBlank(message = "Database URL must not be blank (set DB_URL environment variable)")
    private String url;

    /**
     * The database username.
     */
    @NotBlank(message = "Database username must not be blank (set DB_USERNAME environment variable)")
    private String username;

    /**
     * The database password.
     */
    @NotBlank(message = "Database password must not be blank (set DB_PASSWORD environment variable)")
    private String password;

    /**
     * The fully qualified name of the JDBC driver class.
     */
    private String driverClassName;
}
