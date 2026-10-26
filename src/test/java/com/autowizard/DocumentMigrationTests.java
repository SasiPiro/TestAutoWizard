package com.autowizard;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.migration.JavaMigration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.s3.S3Client;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Migration 4 moves the files of existing documents from the database to object storage. The
 * schema of the application is already migrated, so the test builds a database as it was before
 * (migrations 1 to 3) in a schema of its own, adds a document, then runs the rest.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("dev")
class DocumentMigrationTests {

    static final String SCHEMA = "before_v4";
    static final byte[] PDF = "%PDF-1.4 an old upload".getBytes(StandardCharsets.US_ASCII);

    @Autowired DataSource dataSource;
    @Autowired JdbcTemplate jdbc;
    @Autowired S3Client s3;
    @Autowired List<JavaMigration> javaMigrations;

    @AfterEach
    void dropSchema() {
        jdbc.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
    }

    @Test
    void filesInTheDatabaseAreMovedToObjectStorage() {
        flyway("3").migrate();
        jdbc.update("INSERT INTO before_v4.users (id, email, password) VALUES (1, 'old@test.it', 'x')");
        jdbc.update("""
                INSERT INTO before_v4.vehicles (id, owner_id, type, brand, model, license_plate, vin, mileage,
                                                registration_year, fuel)
                VALUES (1, 1, 'CAR', 'Fiat', 'Panda', 'AB123CD', 'ZFA31200000123456', 1000, 2019, 'BENZINA')""");
        jdbc.update("""
                INSERT INTO before_v4.documents (id, vehicle_id, user_id, name, type, content_type, content)
                VALUES (1, 1, 1, 'polizza.pdf', 'ASSICURAZIONE', 'application/pdf', ?)""", (Object) PDF);

        flyway("latest").migrate();

        String key = jdbc.queryForObject("SELECT storage_key FROM before_v4.documents WHERE id = 1", String.class);
        assertThat(jdbc.queryForObject("SELECT size FROM before_v4.documents WHERE id = 1", Long.class))
                .isEqualTo(PDF.length);
        assertThat(s3.getObjectAsBytes(request -> request.bucket(ObjectStorageContainer.BUCKET).key(key)).asByteArray())
                .isEqualTo(PDF);
        assertThat(jdbc.queryForList("""
                SELECT column_name FROM information_schema.columns
                 WHERE table_schema = 'before_v4' AND table_name = 'documents'""", String.class))
                .doesNotContain("content");
    }

    private Flyway flyway(String target) {
        return Flyway.configure()
                .dataSource(dataSource)
                .schemas(SCHEMA)
                .javaMigrations(javaMigrations.toArray(JavaMigration[]::new))
                .target(target)
                .load();
    }
}
