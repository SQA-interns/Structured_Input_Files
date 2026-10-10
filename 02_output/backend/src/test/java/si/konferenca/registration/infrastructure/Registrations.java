package si.konferenca.registration.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import si.konferenca.registration.domain.AcceptedRegistration;
import si.konferenca.registration.domain.AcceptedRegistration.GivenConsent;
import si.konferenca.registration.domain.AcceptedRegistration.Participant;
import si.konferenca.registration.domain.AcceptedRegistration.SelectedOption;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.RegistrationType;

/** Sample accepted registrations for adapter tests. */
final class Registrations {

  static final UUID REFERENCE = UUID.fromString("3f2b8c1e-6a4d-4e2b-9f1a-0c5d7e8f9a10");
  static final Instant AT = Instant.parse("2026-10-11T08:00:00.123Z");

  private Registrations() {}

  static AcceptedRegistration external() {
    return new AcceptedRegistration(
        REFERENCE,
        AT,
        RegistrationType.EXTERNAL,
        new Participant("Čedomir", "Žagar", "c@example.si", "IJS", null, null, null),
        List.of(
            new SelectedOption("ws-ai", "AI", OptionCategory.WORKSHOP),
            new SelectedOption("ws-sec", "Security", OptionCategory.WORKSHOP),
            new SelectedOption("meal", "Lunch", OptionCategory.MEAL)),
        List.of(new GivenConsent("data", "Data", AT)));
  }

  static AcceptedRegistration student() {
    return new AcceptedRegistration(
        REFERENCE,
        AT,
        RegistrationType.STUDENT,
        new Participant("Luka", "Kranjc", "l@example.si", null, "UM", "RIT", "E1"),
        List.of(),
        List.of(new GivenConsent("data", "Data", AT), new GivenConsent("photos", "Photos", AT)));
  }
}
