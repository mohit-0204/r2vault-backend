package com.mxverse.storage.r2vault.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Configuration for the application DataSource bean.
 * Uses {@link DataSourceProperties} for its settings.
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
