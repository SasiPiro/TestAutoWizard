package com.autowizard.document.internal;

import com.autowizard.document.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    @Query("""
            SELECT new com.autowizard.document.internal.DocumentResponse(d.id, d.name, d.type, d.contentType, d.uploadDate, d.vehicle.id)
              FROM Document d
             WHERE d.vehicle.id = :vehicleId
             ORDER BY d.uploadDate DESC, d.id DESC""")
    List<DocumentResponse> findSummariesByVehicleId(Long vehicleId);

    /** What a download needs: the metadata and where the file is. */
    @Query("""
            SELECT new com.autowizard.document.internal.DocumentRepository$StoredFile(d.name, d.contentType, d.size, d.storageKey)
              FROM Document d JOIN d.vehicle v
             WHERE d.id = :id AND v.owner.id = :ownerId""")
    Optional<StoredFile> findFileByIdAndOwnerId(Long id, Long ownerId);

    @Query("""
            SELECT d.storageKey
              FROM Document d JOIN d.vehicle v
             WHERE d.id = :id AND v.owner.id = :ownerId""")
    Optional<String> findStorageKeyByIdAndOwnerId(Long id, Long ownerId);

    @Modifying
    @Query("DELETE FROM Document d WHERE d.id = :id")
    void deleteMetadataById(Long id);

    record StoredFile(String name, String contentType, long size, String storageKey) {
    }
}
