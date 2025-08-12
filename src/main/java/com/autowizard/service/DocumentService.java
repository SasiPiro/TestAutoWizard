package com.autowizard.service;

import com.autowizard.dto.DocumentDTO;
import com.autowizard.exception.DocumentException;
import com.autowizard.model.Document;
import com.autowizard.model.Vehicle;
import com.autowizard.repository.DocumentRepository;
import com.autowizard.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final VehicleRepository vehicleRepository;

    @Transactional
    public DocumentDTO uploadDocument(MultipartFile file, String type, Long vehicleId, Long userId) {
        log.debug("Uploading document for vehicle {} and user: {}", vehicleId, userId);
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new DocumentException("Vehicle not found"));

        if (!vehicle.getOwner().getId().equals(userId)) {
            throw new DocumentException("Vehicle does not belong to user");
        }

        try {
            Document document = new Document();
            document.setName(file.getOriginalFilename());
            document.setType(type);
            document.setContent(file.getBytes());
            document.setContentType(file.getContentType());
            document.setVehicle(vehicle);
            document.setUploadDate(LocalDateTime.now());

            return convertToDTO(documentRepository.save(document));
        } catch (IOException e) {
            throw new DocumentException("Failed to process document", e);
        }
    }

    @Transactional(readOnly = true)
    public DocumentDTO getDocument(Long id, Long userId) {
        log.debug("Fetching document {} for user: {}", id, userId);
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentException("Document not found"));

        if (!document.getVehicle().getOwner().getId().equals(userId)) {
            throw new DocumentException("Document does not belong to user");
        }

        return convertToDTO(document);
    }

    @Transactional(readOnly = true)
    public List<DocumentDTO> getDocumentsByVehicleId(Long vehicleId, Long userId) {
        log.debug("Fetching documents for vehicle {} and user: {}", vehicleId, userId);
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new DocumentException("Vehicle not found"));

        if (!vehicle.getOwner().getId().equals(userId)) {
            throw new DocumentException("Vehicle does not belong to user");
        }

        return documentRepository.findByVehicleId(vehicleId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteDocument(Long id, Long userId) {
        log.debug("Deleting document {} for user: {}", id, userId);
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentException("Document not found"));

        if (!document.getVehicle().getOwner().getId().equals(userId)) {
            throw new DocumentException("Document does not belong to user");
        }

        documentRepository.delete(document);
    }

    private DocumentDTO convertToDTO(Document document) {
        DocumentDTO dto = new DocumentDTO();
        dto.setId(document.getId());
        dto.setName(document.getName());
        dto.setType(document.getType());
        dto.setContent(document.getContent());
        dto.setContentType(document.getContentType());
        dto.setUploadDate(document.getUploadDate());
        return dto;
    }
} 