package si.konferenca.registration.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** An accepted registration (BR-07); fields of the other type are null. */
public record Registration(
    UUID id,
    Instant registeredAt,
    RegistrationType type,
    String firstName,
    String lastName,
    String email,
    String organization,
    String studyInstitution,
    String studyProgramme,
    String studentId,
    List<SelectedOption> options,
    List<GivenConsent> consents) {

  public Registration {
    options = List.copyOf(options);
    consents = List.copyOf(consents);
  }

  /** An option as configured when the registration was accepted. */
  public record SelectedOption(String id, String name, OptionCategory category) {}

  /** A consent with its wording and the time it was given (SB-14). */
  public record GivenConsent(String id, String text, Instant givenAt) {}
}
