package si.konferenca.registration.infra;

import java.time.Instant;
import java.util.UUID;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.ParticipantDetails;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;
import si.konferenca.registration.domain.SelectedOption;

/** Registrations for unit tests. */
final class Registrations {

  static final Instant AT = Instant.parse("2026-10-12T10:15:30.123456Z");
  static final UUID EXTERNAL_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
  static final UUID STUDENT_ID = UUID.fromString("66666666-7777-8888-9999-000000000000");

  private Registrations() {}

  static Registration external() {
    Registration registration =
        new Registration(
            EXTERNAL_ID,
            RegistrationType.EXTERNAL,
            new ParticipantDetails("Ana", "Novak", "ana@example.org", "Institut", null, null, null),
            AT);
    registration.addOption(new SelectedOption("ws-a", "Workshop A", "WORKSHOP"));
    registration.addOption(new SelectedOption("meal-b", "Lunch", "MEAL"));
    registration.addConsent(new GivenConsent("privacy", "I agree.", AT));
    registration.addConsent(new GivenConsent("photo", "Photos ok.", AT));
    return registration;
  }

  static Registration student() {
    Registration registration =
        new Registration(
            STUDENT_ID,
            RegistrationType.STUDENT,
            new ParticipantDetails(
                "Luka", "Kranjc", "luka@example.org", null, "Univerza", "CS", "6320"),
            AT);
    registration.addConsent(new GivenConsent("privacy", "I agree.", AT));
    return registration;
  }
}
