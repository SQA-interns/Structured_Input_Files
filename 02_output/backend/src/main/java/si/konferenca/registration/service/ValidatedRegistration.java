package si.konferenca.registration.service;

import java.util.List;
import si.konferenca.registration.conference.ConferenceOption;
import si.konferenca.registration.conference.ConsentDefinition;
import si.konferenca.registration.domain.ParticipantDetails;
import si.konferenca.registration.domain.RegistrationType;

/** A submission that passed every field rule: trimmed fields, known options and consents. */
public record ValidatedRegistration(
    RegistrationType type,
    ParticipantDetails participant,
    List<ConferenceOption> options,
    List<ConsentDefinition> consents) {}
