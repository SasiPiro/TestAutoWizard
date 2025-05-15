package com.autowizard.service;

import com.autowizard.dto.DocumentDTO;
import com.autowizard.model.Document;
import com.autowizard.model.Vehicle;
import com.autowizard.model.User;
import com.autowizard.repository.DocumentRepository;
import com.autowizard.repository.VehicleRepository;
import com.autowizard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    @Transactional
    public DocumentDTO uploadDocument(MultipartFile file, String type, Long vehicleId, Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Vehicle vehicle = vehicleRepository.findById(vehicleId)
                    .orElseThrow(() -> new RuntimeException("Vehicle not found"));

            if (!vehicle.getOwner().getId().equals(userId)) {
                throw new RuntimeException("Not authorized to upload document for this vehicle");
            }

            Document document = new Document();
            document.setName(file.getOriginalFilename());
            document.setType(type);
            document.setContentType(file.getContentType());
            document.setContent(file.getBytes());
            document.setUploadDate(LocalDateTime.now());
            document.setVehicle(vehicle);
            document.setUser(user);

            Document savedDocument = documentRepository.save(document);
            return convertToDTO(savedDocument, false);
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + file.getOriginalFilename(), ex);
        }
    }

    @Transactional
    public void deleteDocument(Long id, Long userId) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!document.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to delete this document");
        }

        documentRepository.delete(document);
    }

    public List<DocumentDTO> getDocumentsByVehicleId(Long vehicleId, Long userId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        if (!vehicle.getOwner().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to view documents for this vehicle");
        }

        return documentRepository.findByVehicleId(vehicleId).stream()
                .map(doc -> convertToDTO(doc, false))
                .collect(Collectors.toList());
    }

    public DocumentDTO getDocument(Long id, Long userId) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!document.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to view this document");
        }

        return convertToDTO(document, true);
    }

    private DocumentDTO convertToDTO(Document document, boolean includeContent) {
        DocumentDTO dto = new DocumentDTO();
        dto.setId(document.getId());
        dto.setName(document.getName());
        dto.setType(document.getType());
        dto.setContentType(document.getContentType());
        dto.setUploadDate(document.getUploadDate());
        dto.setVehicleId(document.getVehicle().getId());
        if (includeContent) {
            dto.setContent(document.getContent());
        }
        return dto;
    }
} 