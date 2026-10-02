package br.com.fiap.phase4.commons.domain.exception;

public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message);
    }

    public ResourceNotFoundException(String resourceName, Object id) {
        super("RESOURCE_NOT_FOUND", String.format("%s with identifier '%s' was not found", resourceName, id));
    }
}
