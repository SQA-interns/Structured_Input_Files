package si.konferenca.registration.domain;

import java.util.Optional;

/** The two registration types (BR-01). */
public enum RegistrationType {
  EXTERNAL("External participant"),
  STUDENT("Student");

  private final String label;

  RegistrationType(String label) {
    this.label = label;
  }

  /** Human-readable name used in emails. */
  public String label() {
    return label;
  }

  /** The type with exactly this name, if any. */
  public static Optional<RegistrationType> parse(String value) {
    for (RegistrationType type : values()) {
      if (type.name().equals(value)) {
        return Optional.of(type);
      }
    }
    return Optional.empty();
  }
}
