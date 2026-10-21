package com.autowizard.dto;

import lombok.Data;

@Data
public class DocumentContentDTO {
    private String contentType;
    private byte[] content;
    private String name;
}
