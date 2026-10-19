package si.konferenca.registration.domain;

import java.util.Set;

/** A configured option; {@code availableTo} defaults to both types (OQ-01). */
public record ConferenceOption(
    String id,
    String name,
    OptionCategory category,
    boolean active,
    Set<RegistrationType> availableTo) {

  public boolean isAvailableTo(RegistrationType type) {
    throw new UnsupportedOperationException("not implemented");
  }
}
