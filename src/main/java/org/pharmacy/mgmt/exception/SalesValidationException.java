package org.pharmacy.mgmt.exception;

import java.util.List;

public class SalesValidationException extends RuntimeException {
    private final List<ValidationErrorDetail> errors;

    public SalesValidationException(String message, List<ValidationErrorDetail> errors) {
        super(message);
        this.errors = errors;
    }

    public List<ValidationErrorDetail> getErrors() {
        return errors;
    }
}
