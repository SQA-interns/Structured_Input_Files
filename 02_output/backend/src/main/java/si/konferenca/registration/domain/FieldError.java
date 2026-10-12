package si.konferenca.registration.domain;

import java.io.Serializable;

/** A validation message for one request field. */
public record FieldError(String field, String message) implements Serializable {}
