package com.autowizard.exception;

import org.springframework.http.HttpStatus;

/** An uploaded file whose content is not one of the accepted formats. */
public class UnsupportedDocumentException extends BusinessException {
    public UnsupportedDocumentException(String message) {
        super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported file type", ErrorTypes.UNSUPPORTED_FILE, message);
    }
}
