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

  /** Whether a participant of the type may select this option now (BR-04, D-05). */
  public boolean selectableBy(RegistrationType type) {
    return active && availableTo.contains(type);
  }
}
