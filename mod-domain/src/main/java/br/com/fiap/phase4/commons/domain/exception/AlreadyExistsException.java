package br.com.fiap.phase4.commons.domain.exception;

public class AlreadyExistsException extends DomainException {

    public AlreadyExistsException(String message) {
        super("RESOURCE_ALREADY_EXISTS", message);
    }

    public AlreadyExistsException(String resourceName, Object id) {
        super("RESOURCE_ALREADY_EXISTS", String.format("%s with identifier '%s' already exists", resourceName, id));
    }
}
