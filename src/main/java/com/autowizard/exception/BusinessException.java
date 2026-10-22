package com.autowizard.exception;

import org.springframework.http.HttpStatus;

import java.net.URI;

/**
 * A request rejected for a reason the client can act on (4xx), with the problem details
 * it is reported with: status, title and type.
 *
 * <p>
 * Exceptions are named by meaning (not found, conflict, broken rule), not by entity: the
 * client needs to know what went wrong, and the exception handler maps any of them to a
 * problem detail without a handler per class.
 * </p>
 */
public abstract class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String title;
    private final URI type;

    protected BusinessException(HttpStatus status, String title, URI type, String message) {
        super(message);
        this.status = status;
        this.title = title;
        this.type = type;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    public URI getType() {
        return type;
    }
}
