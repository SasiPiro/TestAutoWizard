package com.autowizard;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * PostgreSQL in Docker for the tests: locks, partial unique indexes and the SQL the queries
 * generate are proven on the database that runs in production, not on an imitation.
 * {@code @ServiceConnection} sets URL and credentials.
 *
 * <p>
 * One database per Spring context, started and stopped with it. Test classes that share a
 * context share its database: tests create their own users and vehicles with random emails,
 * plates and VINs instead of relying on cleanup.
 * </p>
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:16-alpine");
    }
}
