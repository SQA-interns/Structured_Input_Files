package si.konferenca.registration.domain;

import java.util.List;

/** A form that passed validation: trimmed participant fields, resolved options and consents. */
public record ValidRegistration(
    RegistrationType type,
    AcceptedRegistration.Participant participant,
    List<ConferenceOption> options,
    List<Consent> consents) {

  public ValidRegistration {
    options = List.copyOf(options);
    consents = List.copyOf(consents);
  }
}
