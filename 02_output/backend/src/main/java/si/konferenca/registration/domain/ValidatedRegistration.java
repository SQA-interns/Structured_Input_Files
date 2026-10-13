package si.konferenca.registration.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A submission that passed every field rule; trimmed values (BR-02). */
public record ValidatedRegistration(
    RegistrationType type,
    String firstName,
    String lastName,
    String email,
    String organization,
    String studyInstitution,
    String studyProgramme,
    String studentId,
    List<ConferenceOption> options,
    List<ConsentDefinition> consents) {

  public ValidatedRegistration {
    options = List.copyOf(options);
    consents = List.copyOf(consents);
  }

  /** The accepted registration with its id and time; consents are given at that time. */
  public Registration accept(UUID id, Instant registeredAt) {
    return new Registration(
        id,
        registeredAt,
        type,
        firstName,
        lastName,
        email,
        organization,
        studyInstitution,
        studyProgramme,
        studentId,
        options.stream()
            .map(o -> new Registration.SelectedOption(o.id(), o.name(), o.category()))
            .toList(),
        consents.stream()
            .map(c -> new Registration.GivenConsent(c.id(), c.text(), registeredAt))
            .toList());
  }
}
