package si.konferenca.registration.domain;

import java.util.Set;

/** A configured option (workshop, event, meal or other activity). */
public record ConferenceOption(
    String id,
    String name,
    OptionCategory category,
    boolean active,
    Set<RegistrationType> availableTo) {

  public ConferenceOption {
    availableTo = Set.copyOf(availableTo);
  }

  /** Whether a participant of the type may select this option (BR-04, OQ-01). */
  public boolean selectableBy(RegistrationType type) {
    return active && availableTo.contains(type);
  }
}
