package com.autowizard.service;

import com.autowizard.dto.DocumentContentDTO;
import com.autowizard.dto.DocumentDTO;
import com.autowizard.exception.DocumentException;
import com.autowizard.model.Document;
import com.autowizard.model.User;
import com.autowizard.model.Vehicle;
import com.autowizard.repository.DocumentRepository;
import com.autowizard.repository.UserRepository;
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
    private final VehicleAccess vehicleAccess;
    private final UserRepository userRepository;
    @Transactional
    public DocumentDTO uploadDocument(MultipartFile file, Document.DocumentType type, Long vehicleId, Long userId) {
        log.debug("Uploading document for vehicle {} and user: {}", vehicleId, userId);
        Vehicle vehicle = vehicleAccess.requireOwned(vehicleId, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DocumentException("User not found"));

        try {
            Document document = new Document();
            document.setName(file.getOriginalFilename());
            document.setType(type);
            document.setContent(file.getBytes());
            document.setContentType(file.getContentType());
            document.setVehicle(vehicle);
            document.setUploadDate(LocalDateTime.now());
            document.setUser(user);

            return convertToDTO(documentRepository.save(document));
        } catch (IOException e) {
            throw new DocumentException("Failed to process document", e);
        }
    }

    @Transactional(readOnly = true)
    public DocumentContentDTO getDocument(Long id, Long userId) {
        log.debug("Fetching document {} for user: {}", id, userId);
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentException("Document not found"));

        if (!document.getVehicle().getOwner().getId().equals(userId)) {
            throw new DocumentException("Document does not belong to user");
        }

        return convertToContentDTO(document);
    }

    @Transactional(readOnly = true)
    public List<DocumentDTO> getDocumentsByVehicleId(Long vehicleId, Long userId) {
        log.debug("Fetching documents for vehicle {} and user: {}", vehicleId, userId);
        vehicleAccess.requireOwned(vehicleId, userId);

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
        dto.setContentType(document.getContentType());
        dto.setUploadDate(document.getUploadDate());
        dto.setVehicleId(document.getVehicle().getId());
        return dto;
    }

    private DocumentContentDTO convertToContentDTO(Document document) {
        DocumentContentDTO dto = new DocumentContentDTO();
        dto.setName(document.getName());
        dto.setContent(document.getContent());
        dto.setContentType(document.getContentType());
        return dto;
    }
}
