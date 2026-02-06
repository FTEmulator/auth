package com.ftemulator.auth.postgres;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.zaxxer.hikari.HikariDataSource;

@Configuration
@Profile("!test")
public class PostgresConfig {

    private static final Logger log = LoggerFactory.getLogger(PostgresConfig.class);

    @Value("${spring.profile-datasource.url}")
    private String dbUrl;

    @Value("${spring.profile-datasource.username}")
    private String dbUser;

    @Value("${spring.profile-datasource.password}")
    private String dbPassword;

    @Bean
    public DataSource profileDataSource() {
        var ds = new HikariDataSource();
        ds.setJdbcUrl(dbUrl);
        ds.setUsername(dbUser);
        ds.setPassword(dbPassword);
        ds.setDriverClassName("org.postgresql.Driver");
        // sensible defaults; allow overrides via properties
        ds.setMaximumPoolSize(10);
        return ds;
    }

    @Bean
    public ApplicationRunner postgresInitializer(DataSource profileDataSource) {
        return args -> {
            log.info("Applying runtime configuration to PostgreSQL server...");
            try (var conn = profileDataSource.getConnection();
                 var stmt = conn.createStatement()) {

                // Performance - memory
                stmt.execute("ALTER DATABASE profile SET work_mem = '16MB'");
                stmt.execute("ALTER DATABASE profile SET maintenance_work_mem = '128MB'");
                stmt.execute("ALTER DATABASE profile SET effective_cache_size = '256MB'");

                // Performance - planner
                stmt.execute("ALTER DATABASE profile SET random_page_cost = 1.1");
                stmt.execute("ALTER DATABASE profile SET default_statistics_target = 100");

                // Connections
                stmt.execute("ALTER DATABASE profile SET idle_in_transaction_session_timeout = '300s'");
                stmt.execute("ALTER DATABASE profile SET statement_timeout = '60s'");

                // Logging
                stmt.execute("ALTER DATABASE profile SET log_min_duration_statement = 1000");

                log.info("PostgreSQL server configured successfully");
            } catch (Exception e) {
                log.error("Failed to configure PostgreSQL server: {}", e.getMessage());
                throw e;
            }
        };
    }
}