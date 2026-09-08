package com.mxverse.storage.r2vault.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Runs versioned PostgreSQL migrations against the application's custom datasource.
 */
@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    @ConditionalOnProperty(
            prefix = "spring.flyway",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();
    }
}
