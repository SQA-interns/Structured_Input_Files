package si.konferenca.registration.domain;

/** Option categories (BR-04), in display order. */
public enum Category {
  WORKSHOP("Workshops"),
  EVENT("Events"),
  MEAL("Meals"),
  OTHER("Other activities");

  private final String label;

  Category(String label) {
    this.label = label;
  }

  /** Plural heading used in emails and the export. */
  public String label() {
    return label;
  }
}
