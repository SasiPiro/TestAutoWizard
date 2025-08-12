package com.autowizard.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_MESSAGE_PREFIX = "ERR_";
    private static final boolean INCLUDE_STACK_TRACE = false; // Set to false in production

    private ErrorResponse createErrorResponse(HttpStatus status, String message, WebRequest request, String errorCode) {
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        String traceId = UUID.randomUUID().toString();
        
        ErrorResponse error = new ErrorResponse(
            status.value(),
            message,
            System.currentTimeMillis()
        );
        error.setPath(path);
        error.setTraceId(traceId);
        error.setErrorCode(errorCode);
        
        // Log dell'errore con dettagli strutturati
        Map<String, Object> logDetails = new HashMap<>();
        logDetails.put("traceId", traceId);
        logDetails.put("path", path);
        logDetails.put("status", status.value());
        logDetails.put("errorCode", errorCode);
        logDetails.put("timestamp", LocalDateTime.now());
        
        log.error("Error occurred: {}", logDetails);
        
        return error;
    }

    @ExceptionHandler(ReminderException.class)
    public ResponseEntity<ErrorResponse> handleReminderException(ReminderException ex, WebRequest request) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, ERROR_MESSAGE_PREFIX + "REMINDER"));
    }

    @ExceptionHandler(MaintenanceTaskException.class)
    public ResponseEntity<ErrorResponse> handleMaintenanceTaskException(MaintenanceTaskException ex, WebRequest request) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, ERROR_MESSAGE_PREFIX + "TASK"));
    }

    @ExceptionHandler(VehicleException.class)
    public ResponseEntity<ErrorResponse> handleVehicleException(VehicleException ex, WebRequest request) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, ERROR_MESSAGE_PREFIX + "VEHICLE"));
    }

    @ExceptionHandler(DocumentException.class)
    public ResponseEntity<ErrorResponse> handleDocumentException(DocumentException ex, WebRequest request) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, ERROR_MESSAGE_PREFIX + "DOCUMENT"));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(EntityNotFoundException ex, WebRequest request) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(createErrorResponse(HttpStatus.NOT_FOUND, "Risorsa non trovata", request, ERROR_MESSAGE_PREFIX + "NOT_FOUND"));
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<ErrorResponse> handleEmptyResultDataAccessException(EmptyResultDataAccessException ex, WebRequest request) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(createErrorResponse(HttpStatus.NOT_FOUND, "Risorsa non trovata", request, ERROR_MESSAGE_PREFIX + "NOT_FOUND"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        ErrorResponse error = createErrorResponse(HttpStatus.BAD_REQUEST, "Errore di validazione", request, ERROR_MESSAGE_PREFIX + "VALIDATION");
        
        ex.getBindingResult().getAllErrors().forEach((err) -> {
            String fieldName = ((FieldError) err).getField();
            String errorMessage = err.getDefaultMessage();
            error.addDetail(fieldName + ": " + errorMessage);
        });
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException ex, WebRequest request) {
        ErrorResponse error = createErrorResponse(HttpStatus.BAD_REQUEST, "Errore di validazione dei vincoli", request, ERROR_MESSAGE_PREFIX + "CONSTRAINT");
        
        ex.getConstraintViolations().forEach(violation -> {
            error.addDetail(violation.getPropertyPath() + ": " + violation.getMessage());
        });
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex, WebRequest request) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(createErrorResponse(HttpStatus.CONFLICT, "Violazione dei vincoli di integrità dei dati", request, ERROR_MESSAGE_PREFIX + "INTEGRITY"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, WebRequest request) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(createErrorResponse(HttpStatus.BAD_REQUEST, "Richiesta non valida", request, ERROR_MESSAGE_PREFIX + "INVALID_REQUEST"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex, WebRequest request) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(createErrorResponse(HttpStatus.BAD_REQUEST, "Tipo di parametro non valido", request, ERROR_MESSAGE_PREFIX + "TYPE_MISMATCH"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex, WebRequest request) {
        return ResponseEntity
            .status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(createErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, "Metodo non supportato", request, ERROR_MESSAGE_PREFIX + "METHOD_NOT_ALLOWED"));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(NoHandlerFoundException ex, WebRequest request) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(createErrorResponse(HttpStatus.NOT_FOUND, "Endpoint non trovato", request, ERROR_MESSAGE_PREFIX + "NOT_FOUND"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(createErrorResponse(HttpStatus.UNAUTHORIZED, "Errore di autenticazione", request, ERROR_MESSAGE_PREFIX + "AUTH"));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(createErrorResponse(HttpStatus.UNAUTHORIZED, "Credenziali non valide", request, ERROR_MESSAGE_PREFIX + "BAD_CREDENTIALS"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(createErrorResponse(HttpStatus.FORBIDDEN, "Accesso negato", request, ERROR_MESSAGE_PREFIX + "FORBIDDEN"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        // In produzione, non esporre i dettagli dell'errore
        String message = INCLUDE_STACK_TRACE ? 
            "Si è verificato un errore imprevisto: " + ex.getMessage() :
            "Si è verificato un errore imprevisto. Riprova più tardi.";

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, message, request, ERROR_MESSAGE_PREFIX + "INTERNAL"));
    }
} 