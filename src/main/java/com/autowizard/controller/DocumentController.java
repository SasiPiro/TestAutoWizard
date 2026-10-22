package com.autowizard.controller;

import com.autowizard.dto.DocumentContent;
import com.autowizard.dto.DocumentResponse;
import com.autowizard.model.Document;
import com.autowizard.security.CurrentUser;
import com.autowizard.security.UserPrincipal;
import com.autowizard.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") Document.DocumentType type,
            @RequestParam("vehicleId") Long vehicleId,
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(documentService.uploadDocument(file, type, vehicleId, currentUser.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable Long id,
            @CurrentUser UserPrincipal currentUser) {
        documentService.deleteDocument(id, currentUser.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<DocumentResponse>> getDocumentsByVehicle(
            @PathVariable Long vehicleId,
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(documentService.getDocumentsByVehicleId(vehicleId, currentUser.getId()));
    }

    /**
     * The file as an attachment, with the content type detected at upload (never the one
     * the client declared) and the name encoded for any character set. Spring Security adds
     * {@code X-Content-Type-Options: nosniff}, so browsers do not guess another type.
     */
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> downloadDocument(
            @PathVariable Long id,
            @CurrentUser UserPrincipal currentUser) {
        DocumentContent document = documentService.getDocument(id, currentUser.getId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(document.contentType()));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(document.name(), StandardCharsets.UTF_8)
                .build());
        headers.setContentLength(document.content().length);
        return ResponseEntity.ok()
                .headers(headers)
                .body(document.content());
    }
}
