package com.autowizard.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DocumentDTO {
    private Long id;
    private String name;
    private String type;
    private String contentType;
    private LocalDateTime uploadDate;
    private Long vehicleId;
    private byte[] content; // Optional, only populated when downloading
} 