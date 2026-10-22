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
import java.util.stream.Collectors;

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

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Document document = new Document();
        document.setName(file.getOriginalFilename());
        document.setType(type);
        document.setContent(readContent(file));
        document.setContentType(file.getContentType());
        document.setVehicle(vehicle);
        document.setUploadDate(LocalDateTime.now());
        document.setUser(user);

        Document saved = documentRepository.save(document);
        log.info("[DOCUMENT_UPLOAD] outcome=success documentId={} vehicleId={} type={} contentType={} size={}",
                saved.getId(), vehicleId, type, saved.getContentType(), file.getSize());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public DocumentContent getDocument(Long id, Long userId) {
        Document document = documentRepository.findByIdAndVehicleOwnerId(id, userId)
                .orElseThrow(() -> new NotFoundException("Document not found"));

        return toContent(document);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByVehicleId(Long vehicleId, Long userId) {
        vehicleAccess.requireOwned(vehicleId, userId);

        return documentRepository.findByVehicleId(vehicleId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteDocument(Long id, Long userId) {
        Document document = documentRepository.findByIdAndVehicleOwnerId(id, userId)
                .orElseThrow(() -> new NotFoundException("Document not found"));

        documentRepository.delete(document);
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

    private DocumentContent toContent(Document document) {
        return new DocumentContent(document.getName(), document.getContentType(), document.getContent());
    }
}
