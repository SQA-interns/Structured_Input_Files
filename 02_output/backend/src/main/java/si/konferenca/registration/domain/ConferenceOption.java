package si.konferenca.registration.domain;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** A configured option; {@code availableTo} defaults to both types (OQ-01). */
public record ConferenceOption(
    String id,
    String name,
    OptionCategory category,
    boolean active,
    Set<RegistrationType> availableTo) {

  public ConferenceOption {
    availableTo =
        availableTo == null || availableTo.isEmpty()
            ? Collections.unmodifiableSet(EnumSet.allOf(RegistrationType.class))
            : Collections.unmodifiableSet(EnumSet.copyOf(availableTo));
  }

  public boolean isAvailableTo(RegistrationType type) {
    return availableTo.contains(type);
  }
}
