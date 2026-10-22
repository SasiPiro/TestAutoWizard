package com.autowizard.service;

import com.autowizard.dto.DocumentContent;
import com.autowizard.dto.DocumentResponse;
import com.autowizard.exception.NotFoundException;
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
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final VehicleAccess vehicleAccess;
    private final UserRepository userRepository;

    @Transactional
    public DocumentResponse uploadDocument(MultipartFile file, Document.DocumentType type, Long vehicleId, Long userId) {
        Vehicle vehicle = vehicleAccess.requireOwned(vehicleId, userId);
        byte[] content = readContent(file);
        String contentType = DocumentFiles.detectContentType(content);

        User user = userRepository.getReferenceById(userId);

        Document document = new Document();
        document.setName(DocumentFiles.safeName(file.getOriginalFilename()));
        document.setType(type);
        document.setContent(content);
        document.setContentType(contentType);
        document.setVehicle(vehicle);
        document.setUploadDate(LocalDateTime.now());
        document.setUser(user);

        Document saved = documentRepository.save(document);
        log.info("[DOCUMENT_UPLOAD] outcome=success documentId={} vehicleId={} type={} contentType={} size={}",
                saved.getId(), vehicleId, type, contentType, content.length);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public DocumentContent getDocument(Long id, Long userId) {
        return documentRepository.findContentByIdAndOwnerId(id, userId)
                .orElseThrow(() -> new NotFoundException("Document not found"));
    }

    /** Metadata only: the file contents are not read. */
    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByVehicleId(Long vehicleId, Long userId) {
        vehicleAccess.requireOwned(vehicleId, userId);
        return documentRepository.findSummariesByVehicleId(vehicleId);
    }

    @Transactional
    public void deleteDocument(Long id, Long userId) {
        // Checked and deleted without loading the entity, which would load the file
        if (!documentRepository.existsByIdAndVehicleOwnerId(id, userId)) {
            throw new NotFoundException("Document not found");
        }
        documentRepository.deleteMetadataAndContentById(id);
        log.info("[DOCUMENT_DELETE] outcome=success documentId={}", id);
    }

    private static byte[] readContent(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            // The upload could not be read on the server side: a 500, not the client's fault
            throw new UncheckedIOException(e);
        }
    }

    private DocumentResponse toResponse(Document document) {
        return new DocumentResponse(document.getId(), document.getName(), document.getType(),
                document.getContentType(), document.getUploadDate(), document.getVehicle().getId());
    }
}
