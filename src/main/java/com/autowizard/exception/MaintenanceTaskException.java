package com.autowizard.exception;

public class MaintenanceTaskException extends RuntimeException {
    public MaintenanceTaskException(String message) {
        super(message);
    }

    public MaintenanceTaskException(String message, Throwable cause) {
        super(message, cause);
    }
}