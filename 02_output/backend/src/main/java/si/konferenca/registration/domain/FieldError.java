package si.konferenca.registration.domain;

/** A rejected field and the reason, shown to the participant. */
public record FieldError(String field, String message) {}
