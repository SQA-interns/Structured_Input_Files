package si.konferenca.registration.domain;

/** One field error of a rejected submission (openapi.json ErrorResponse.fieldErrors). */
public record FieldError(String field, String message) {}
