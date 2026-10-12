package si.konferenca.registration.service;

/** A rejected field and a message for the participant. */
public record FieldError(String field, String message) {}
