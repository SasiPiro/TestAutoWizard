package com.autowizard.document.internal;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.List;

/**
 * Flyway migration 4: the files leave the {@code documents.content} column for object storage.
 * Written in Java because SQL cannot write to the store; Spring Boot hands Flyway every
 * {@code JavaMigration} bean, so it gets the same {@link DocumentStorage} as the application.
 *
 * <p>
 * Every file is copied to the store and its key recorded, then the column is dropped, all in the
 * migration's transaction: if anything fails, the schema and the rows are rolled back (PostgreSQL
 * DDL is transactional) and the migration runs again at the next start. Files already copied by
 * the failed run are left behind, unreferenced.
 * </p>
 * <p>
 * One file at a time is held in memory. This suits the data of this application; for a large table
 * the copy would be a separate job run before the deployment, and the migration would only check
 * that every row has a key.
 * </p>
 */
@Slf4j
@Component
class V4__Move_document_files_to_object_storage extends BaseJavaMigration {

    private final DocumentStorage storage;

    V4__Move_document_files_to_object_storage(DocumentStorage storage) {
        this.storage = storage;
    }

    @Override
    public void migrate(Context context) {
        JdbcTemplate jdbc = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));

        jdbc.execute("ALTER TABLE documents ADD COLUMN storage_key VARCHAR(100), ADD COLUMN size BIGINT");

        List<Long> ids = jdbc.queryForList("SELECT id FROM documents ORDER BY id", Long.class);
        for (Long id : ids) {
            jdbc.query("SELECT content_type, content FROM documents WHERE id = ?", row -> {
                byte[] content = row.getBytes("content");
                String key = storage.put(new ByteArrayInputStream(content), content.length, row.getString("content_type"));
                jdbc.update("UPDATE documents SET storage_key = ?, size = ? WHERE id = ?", key, content.length, id);
            }, id);
        }

        jdbc.execute("""
                ALTER TABLE documents
                    DROP COLUMN content,
                    ALTER COLUMN storage_key SET NOT NULL,
                    ALTER COLUMN size SET NOT NULL,
                    ADD CONSTRAINT uk_documents_storage_key UNIQUE (storage_key),
                    ADD CONSTRAINT ck_documents_size CHECK (size > 0)""");
        log.info("[DOCUMENT_MIGRATION] outcome=success movedFiles={}", ids.size());
    }
}
