package si.konferenca.registration.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A submission that passed every business rule; values are trimmed. */
public record ValidSubmission(
    RegistrationType type,
    Participant participant,
    List<ConferenceOption> options,
    List<ConsentDefinition> consents,
    String captchaToken) {

  /** The accepted registration; consents are given at the registration time. */
  public Registration accept(UUID id, Instant at) {
    return new Registration(
        id,
        type,
        at,
        participant,
        options.stream().map(o -> new SelectedOption(o.id(), o.name(), o.category())).toList(),
        consents.stream().map(c -> new GivenConsent(c.id(), c.text(), at)).toList());
  }
}
