package com.autowizard.dto;

/** A file to download: returned as the response body, never serialized as JSON. */
public record DocumentContent(String name, String contentType, byte[] content) {
}
