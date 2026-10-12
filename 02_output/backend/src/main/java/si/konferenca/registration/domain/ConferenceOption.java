package si.konferenca.registration.domain;

import java.util.Set;

/** A configurable workshop, event, meal or other activity (US-003). */
public record ConferenceOption(
    String id, String name, Category category, boolean active, Set<RegistrationType> types) {

  public ConferenceOption {
    types = Set.copyOf(types);
  }

  /** True if participants of this type may select the option now (BR-04, D-10). */
  public boolean selectableBy(RegistrationType type) {
    return active && types.contains(type);
  }
}
