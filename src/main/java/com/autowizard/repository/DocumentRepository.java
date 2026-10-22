package com.autowizard.repository;

import com.autowizard.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByVehicleId(Long vehicleId);
    List<Document> findByUserId(Long userId);
    Optional<Document> findByIdAndVehicleOwnerId(Long id, Long ownerId);
} 