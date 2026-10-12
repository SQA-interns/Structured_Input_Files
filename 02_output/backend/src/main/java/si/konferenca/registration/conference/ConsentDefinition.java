package si.konferenca.registration.conference;

/** A configured consent with its wording (OQ-02). */
public record ConsentDefinition(String id, String text, boolean mandatory) {}
