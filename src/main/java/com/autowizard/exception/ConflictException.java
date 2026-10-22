package com.autowizard.exception;

import org.springframework.http.HttpStatus;

/** The request conflicts with existing data, e.g. an email or a license plate already registered. */
public class ConflictException extends BusinessException {
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, "Conflict with existing data", ErrorTypes.CONFLICT, message);
    }
}
