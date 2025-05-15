package com.autowizard.controller;

import com.autowizard.dto.DocumentDTO;
import com.autowizard.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<DocumentDTO> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type,
            @RequestParam("vehicleId") Long vehicleId,
            @RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(documentService.uploadDocument(file, type, vehicleId, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id, @RequestAttribute("userId") Long userId) {
        documentService.deleteDocument(id, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<DocumentDTO>> getDocumentsByVehicle(
            @PathVariable Long vehicleId,
            @RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(documentService.getDocumentsByVehicleId(vehicleId, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> downloadDocument(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId) {
        DocumentDTO document = documentService.getDocument(id, userId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(document.getContentType()));
        headers.setContentDispositionFormData("attachment", document.getName());
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(document.getContent());
    }
} 