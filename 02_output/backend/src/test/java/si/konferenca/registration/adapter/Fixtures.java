package si.konferenca.registration.adapter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Participant;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;
import si.konferenca.registration.domain.SelectedOption;

/** Registrations for adapter unit tests. */
public final class Fixtures {

  public static final Instant AT = Instant.parse("2026-10-11T08:30:00Z");

  private Fixtures() {}

  public static Registration external() {
    return new Registration(
        UUID.fromString("11111111-2222-3333-4444-555555555555"),
        RegistrationType.EXTERNAL,
        AT,
        new Participant("Črt", "Žagar", "crt@example.si", "IJS", null, null, null),
        List.of(
            new SelectedOption("ws", "Delavnica", OptionCategory.WORKSHOP),
            new SelectedOption("lunch", "Kosilo", OptionCategory.MEAL),
            new SelectedOption("dinner", "Večerja", OptionCategory.MEAL)),
        List.of(new GivenConsent("privacy", "Privacy text", AT)));
  }

  public static Registration student() {
    return new Registration(
        UUID.fromString("66666666-7777-8888-9999-000000000000"),
        RegistrationType.STUDENT,
        AT,
        new Participant("Luka", "Kranjc", "luka@example.si", null, "UL", "RI", "632"),
        List.of(),
        List.of());
  }
}
