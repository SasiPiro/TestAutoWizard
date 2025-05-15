package com.autowizard.repository;

import com.autowizard.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByVehicleId(Long vehicleId);
    List<Document> findByUserId(Long userId);
} 