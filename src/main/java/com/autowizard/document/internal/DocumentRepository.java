package com.autowizard.document.internal;

import com.autowizard.document.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Queries on documents select either the metadata or the content, never both by accident:
 * {@code @Basic(fetch = LAZY)} on the content is ignored without bytecode enhancement, so
 * loading a {@link Document} entity always loads the file.
 */
public interface DocumentRepository extends JpaRepository<Document, Long> {

    @Query("""
            SELECT new com.autowizard.document.internal.DocumentResponse(d.id, d.name, d.type, d.contentType, d.uploadDate, d.vehicle.id)
              FROM Document d
             WHERE d.vehicle.id = :vehicleId
             ORDER BY d.uploadDate DESC, d.id DESC""")
    List<DocumentResponse> findSummariesByVehicleId(Long vehicleId);

    @Query("""
            SELECT new com.autowizard.document.internal.DocumentContent(d.name, d.contentType, d.content)
              FROM Document d JOIN d.vehicle v
             WHERE d.id = :id AND v.owner.id = :ownerId""")
    Optional<DocumentContent> findContentByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByIdAndVehicleOwnerId(Long id, Long ownerId);

    @Modifying
    @Query("DELETE FROM Document d WHERE d.id = :id")
    void deleteMetadataAndContentById(Long id);
}
