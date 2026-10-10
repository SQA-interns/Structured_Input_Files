package si.konferenca.registration.domain;

/** Option groups of BR-04. */
public enum OptionCategory {
  WORKSHOP("Workshop", "Workshops"),
  EVENT("Event", "Events"),
  MEAL("Meal", "Meals"),
  OTHER("Other activity", "Other activities");

  private final String label;
  private final String pluralLabel;

  OptionCategory(String label, String pluralLabel) {
    this.label = label;
    this.pluralLabel = pluralLabel;
  }

  public String label() {
    return label;
  }

  public String pluralLabel() {
    return pluralLabel;
  }
}
