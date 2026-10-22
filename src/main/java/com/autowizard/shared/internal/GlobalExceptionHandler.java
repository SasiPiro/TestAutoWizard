package com.autowizard.shared.internal;

import com.autowizard.shared.BusinessException;
import com.autowizard.shared.ErrorTypes;
import com.autowizard.shared.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

/**
 * Turns every exception into an RFC 7807 problem detail with the status that matches its
 * meaning, and logs it once.
 *
 * <p>
 * Spring MVC's own exceptions (unknown route, unreadable body, wrong parameter type,
 * unsupported method, ...) are handled by {@link ResponseEntityExceptionHandler} with
 * their standard status; {@link #handleExceptionInternal} adds the common properties and
 * the log line to those too. A 500 never contains the exception message: it may hold SQL,
 * host names or other internals.
 * </p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    static final String INVALID_CREDENTIALS = "Invalid email or password";

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(BusinessException ex, HttpServletRequest request) {
        return clientError(ex, ex.getStatus(), ex.getTitle(), ex.getType(), ex.getMessage(), request);
    }

    /**
     * Login failures. One generic message for unknown email and wrong password, so the
     * endpoint cannot be used to find out which emails are registered. Already logged
     * with the attempted email by AuthService: no second line here.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", ErrorTypes.UNAUTHORIZED, INVALID_CREDENTIALS);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return clientError(ex, HttpStatus.FORBIDDEN, "Forbidden", ErrorTypes.FORBIDDEN, "Access denied", request);
    }

    /** A unique constraint caught what a service check missed (e.g. two concurrent requests). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        return clientError(ex, HttpStatus.CONFLICT, "Conflict with existing data", ErrorTypes.CONFLICT,
                "The request conflicts with existing data", request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        // The only ERROR with stack trace: a real bug or an infrastructure failure
        log.error("[HTTP_SERVER_ERROR] status=500 method={} path={} exception={}",
                request.getMethod(), request.getRequestURI(), ex.getClass().getName(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", ErrorTypes.INTERNAL,
                "An unexpected error occurred. Contact support if the problem persists");
    }

    /** Bean Validation on a request body: 400 with the message of each invalid field. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new TreeMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage() != null ? error.getDefaultMessage() : "invalid");
        }
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problemDetail.setTitle("Invalid input data");
        problemDetail.setType(ErrorTypes.VALIDATION);
        problemDetail.setProperty("errors", errors);
        return handleExceptionInternal(ex, problemDetail, headers, HttpStatus.BAD_REQUEST, request);
    }

    /** Spring MVC exceptions: same properties and the same single log line as ours. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        if (statusCode.is5xxServerError()) {
            log.error("[HTTP_SERVER_ERROR] status={} exception={}", statusCode.value(), ex.getClass().getName(), ex);
        } else {
            log.info("[HTTP_CLIENT_ERROR] status={} exception={}", statusCode.value(), ex.getClass().getSimpleName());
        }
        if (body instanceof ProblemDetail problemDetail) {
            addCommonProperties(problemDetail);
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    // 4xx = the client sent something the API rejects: expected, not an application error.
    // INFO without stack trace, so WARN and ERROR stay meaningful.
    private ProblemDetail clientError(Exception ex, HttpStatus status, String title, URI type, String detail,
                                      HttpServletRequest request) {
        log.info("[HTTP_CLIENT_ERROR] status={} method={} path={} exception={} detail=\"{}\"",
                status.value(), request.getMethod(), request.getRequestURI(), ex.getClass().getSimpleName(), detail);
        return problem(status, title, type, detail);
    }

    private static ProblemDetail problem(HttpStatus status, String title, URI type, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setType(type);
        addCommonProperties(problemDetail);
        return problemDetail;
    }

    private static void addCommonProperties(ProblemDetail problemDetail) {
        problemDetail.setProperty("timestamp", Instant.now());
        // The same id as in the response header and in every log line of this request
        String requestId = MDC.get(RequestIdFilter.MDC_KEY);
        if (requestId != null) {
            problemDetail.setProperty("requestId", requestId);
        }
    }
}
