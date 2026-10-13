package si.konferenca.registration.domain;

import java.util.Optional;

/** Option categories (BR-04). */
public enum OptionCategory {
  WORKSHOP("Workshop", "Workshops"),
  EVENT("Event", "Events"),
  MEAL("Meal", "Meals"),
  OTHER("Other activity", "Other activities");

  private final String singular;
  private final String plural;

  OptionCategory(String singular, String plural) {
    this.singular = singular;
    this.plural = plural;
  }

  /** Name of one option's category, e.g. in emails. */
  public String singular() {
    return singular;
  }

  /** Name of the group, e.g. as an export column. */
  public String plural() {
    return plural;
  }

  /** The category with exactly this name, if any. */
  public static Optional<OptionCategory> parse(String value) {
    for (OptionCategory category : values()) {
      if (category.name().equals(value)) {
        return Optional.of(category);
      }
    }
    return Optional.empty();
  }
}
