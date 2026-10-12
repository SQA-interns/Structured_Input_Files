package si.konferenca.registration.domain;

/** The two registration forms (BR-01). */
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
}
