package si.konferenca.registration.domain;

/** A validation error for one field of the submission (`openapi.yaml` FieldError). */
public record FieldError(String field, String message) {}
