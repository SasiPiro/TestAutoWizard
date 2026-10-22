package com.autowizard.exception;

import org.springframework.http.HttpStatus;

/** A well-formed request that a domain rule rejects, e.g. a mileage lower than the current one. */
public class BusinessRuleException extends BusinessException {
    public BusinessRuleException(String message) {
        super(HttpStatus.BAD_REQUEST, "Business rule violated", ErrorTypes.BUSINESS_RULE, message);
    }
}
