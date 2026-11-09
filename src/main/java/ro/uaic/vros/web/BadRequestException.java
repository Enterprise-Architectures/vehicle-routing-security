package ro.uaic.vros.web;

/** A request is syntactically valid JSON, but violates a domain rule. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { super(message); }
}
