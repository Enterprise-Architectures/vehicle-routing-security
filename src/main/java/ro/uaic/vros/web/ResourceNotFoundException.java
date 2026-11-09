package ro.uaic.vros.web;

/**
 * LAB 5 HOMEWORK 3: Domain-specific runtime exception.
 * Throw from services when a requested ID does not exist.
 * ApiExceptionHandler translates it consistently to HTTP 404.
 */
public class ResourceNotFoundException extends IllegalArgumentException {
    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " with id " + id + " was not found");
    }
}
