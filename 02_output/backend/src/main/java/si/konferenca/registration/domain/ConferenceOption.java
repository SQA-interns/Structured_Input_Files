package si.konferenca.registration.domain;

import java.util.Set;

/** A configured workshop, event, meal or other activity (US-003). */
public record ConferenceOption(
    String id,
    String name,
    OptionCategory category,
    boolean active,
    Set<RegistrationType> availableTo) {

  public ConferenceOption {
    availableTo = Set.copyOf(availableTo);
  }

  /** Active and offered to the given registration type (BR-04, OQ-01). */
  public boolean selectableBy(RegistrationType type) {
    return active && availableTo.contains(type);
  }
}
