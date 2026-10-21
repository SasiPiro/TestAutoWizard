package com.autowizard.controller;

import com.autowizard.dto.DocumentContentDTO;
import com.autowizard.dto.DocumentDTO;
import com.autowizard.model.Document;
import com.autowizard.security.CurrentUser;
import com.autowizard.security.UserPrincipal;
import com.autowizard.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<DocumentDTO> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") Document.DocumentType type,
            @RequestParam("vehicleId") Long vehicleId,
            @CurrentUser UserPrincipal currentUser) {
        log.debug("Uploading document for user: {}", currentUser);
        return ResponseEntity.ok(documentService.uploadDocument(file, type, vehicleId, currentUser.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable Long id,
            @CurrentUser UserPrincipal currentUser) {
        log.debug("Deleting document {} for user: {}", id, currentUser.getId());
        documentService.deleteDocument(id, currentUser.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<DocumentDTO>> getDocumentsByVehicle(
            @PathVariable Long vehicleId,
            @CurrentUser UserPrincipal currentUser) {
        log.debug("Fetching documents for vehicle {} and user: {}", vehicleId, currentUser.getId());
        return ResponseEntity.ok(documentService.getDocumentsByVehicleId(vehicleId, currentUser.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> downloadDocument(
            @PathVariable Long id,
            @CurrentUser UserPrincipal currentUser) {
        log.debug("Downloading document {} for user: {}", id, currentUser.getId());
        DocumentContentDTO document = documentService.getDocument(id, currentUser.getId());
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(document.getContentType()));
        headers.setContentDispositionFormData("attachment", document.getName());
        return ResponseEntity.ok()
                .headers(headers)
                .body(document.getContent());
    }
}
