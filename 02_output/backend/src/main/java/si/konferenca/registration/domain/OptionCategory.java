package si.konferenca.registration.domain;

/** Groups of conference options (BR-04). */
public enum OptionCategory {
  WORKSHOP("Workshop"),
  EVENT("Event"),
  MEAL("Meal"),
  OTHER("Other activity");

  private final String label;

  OptionCategory(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
