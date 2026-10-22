package com.autowizard.dto;

import com.autowizard.model.Document;

import java.time.LocalDateTime;

/** Document metadata, without the file content. */
public record DocumentResponse(
        Long id,
        String name,
        Document.DocumentType type,
        String contentType,
        LocalDateTime uploadDate,
        Long vehicleId) {
}
