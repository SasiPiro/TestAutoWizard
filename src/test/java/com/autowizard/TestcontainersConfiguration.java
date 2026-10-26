package com.autowizard;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * PostgreSQL and an S3-compatible object store in Docker for the tests: locks, partial unique
 * indexes, the SQL the queries generate and the storage of the files are proven on the same kind
 * of systems that run in production, not on imitations. {@code @ServiceConnection} sets the
 * database URL and credentials; the store has no service connection, so its properties are
 * registered from the container.
 *
 * <p>
 * One database and one store per Spring context, started and stopped with it. Test classes that
 * share a context share them: tests create their own users and vehicles with random emails,
 * plates and VINs instead of relying on cleanup, and files get random keys.
 * </p>
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:16-alpine");
    }

    @Bean
    ObjectStorageContainer objectStorage() {
        return new ObjectStorageContainer();
    }

    @Bean
    DynamicPropertyRegistrar objectStorageProperties(ObjectStorageContainer objectStorage) {
        return registry -> {
            registry.add("app.storage.endpoint", objectStorage::endpoint);
            registry.add("app.storage.bucket", () -> ObjectStorageContainer.BUCKET);
            registry.add("app.storage.access-key", () -> ObjectStorageContainer.ACCESS_KEY);
            registry.add("app.storage.secret-key", () -> ObjectStorageContainer.SECRET_KEY);
            registry.add("app.storage.path-style", () -> "true");
        };
    }
}
