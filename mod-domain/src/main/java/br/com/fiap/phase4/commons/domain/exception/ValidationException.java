package br.com.fiap.phase4.commons.domain.exception;

import java.util.Collections;
import java.util.List;

public class ValidationException extends DomainException {

    private final List<String> errors;

    public ValidationException(String message) {
        super("VALIDATION_ERROR", message);
        this.errors = List.of(message);
    }

    public ValidationException(String message, List<String> errors) {
        super("VALIDATION_ERROR", message);
        this.errors = errors != null ? List.copyOf(errors) : Collections.emptyList();
    }

    public List<String> getErrors() {
        return errors;
    }
}
