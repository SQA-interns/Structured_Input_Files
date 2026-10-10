package si.konferenca.registration.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * A selectable workshop, event, meal or other activity (BR-04), with the registration types that
 * may select it (D-08).
 */
public record ConferenceOption(
    String id,
    String name,
    OptionCategory category,
    boolean active,
    Set<RegistrationType> audience) {

  public ConferenceOption {
    audience =
        audience == null || audience.isEmpty()
            ? Set.copyOf(EnumSet.allOf(RegistrationType.class))
            : Set.copyOf(audience);
  }

  /** Whether a participant of the given type may select this option now. */
  public boolean selectableBy(RegistrationType type) {
    return active && audience.contains(type);
  }
}
