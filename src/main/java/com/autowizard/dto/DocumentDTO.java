package com.autowizard.dto;

import com.autowizard.model.Document;
import lombok.Data;
import java.time.LocalDateTime;
//TODO CREARE UN ALTRO DTO PER IL CONTENT
@Data
public class DocumentDTO {
    private Long id;
    private String name;
    private Document.DocumentType type;
    private String contentType;
    private LocalDateTime uploadDate;
    private Long vehicleId; // Optional, only populated when downloading
}
