package com.mxverse.storage.r2vault.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Configuration class for the primary data source.
 * <p>
 * This class instantiates a {@link DataSource} bean based on the validated
 * {@link DataSourceProperties}. It ensures that the application has a reliable
 * connection to the PostgreSQL database.
 *
 * @see DataSourceProperties
 */
@Configuration
@RequiredArgsConstructor
public class DatabaseConfig {

    private final DataSourceProperties dataSourceProperties;

    /**
     * Creates and configures the DataSource bean using the validated properties.
     *
     * @return A configured DataSource instance.
     */
    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .url(dataSourceProperties.getUrl())
                .username(dataSourceProperties.getUsername())
                .password(dataSourceProperties.getPassword())
                .driverClassName(dataSourceProperties.getDriverClassName())
                .build();
    }
}
