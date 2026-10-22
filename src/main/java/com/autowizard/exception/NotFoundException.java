package com.autowizard.exception;

import org.springframework.http.HttpStatus;

/**
 * The resource does not exist, or it belongs to another user: both are 404, so that a
 * caller cannot find out whether someone else's resource exists.
 */
public class NotFoundException extends BusinessException {
    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "Resource not found", ErrorTypes.NOT_FOUND, message);
    }
}
