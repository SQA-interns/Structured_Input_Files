package si.konferenca.registration.domain;

/** A consent the participant can give; mandatory ones must be given (BR-05, D-09). */
public record Consent(String id, String text, boolean mandatory) {}
