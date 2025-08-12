package com.autowizard.exception;

public class ReminderException extends RuntimeException {
    public ReminderException(String message) {
        super(message);
    }

    public ReminderException(String message, Throwable cause) {
        super(message, cause);
    }
} 