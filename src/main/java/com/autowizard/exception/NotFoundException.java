package com.autowizard.exception;

/**
 * The resource does not exist, or it belongs to another user: both are 404, so that a
 * caller cannot find out whether someone else's resource exists.
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
