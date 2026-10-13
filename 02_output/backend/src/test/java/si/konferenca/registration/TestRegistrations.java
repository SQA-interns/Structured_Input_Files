package si.konferenca.registration;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.Registration.GivenConsent;
import si.konferenca.registration.domain.Registration.SelectedOption;
import si.konferenca.registration.domain.RegistrationType;

/** Accepted registrations for unit tests. */
public final class TestRegistrations {

  public static final Instant AT = Instant.parse("2026-10-13T20:15:03.120Z");

  private TestRegistrations() {}

  public static Registration external() {
    return new Registration(
        UUID.fromString("0b8f3c1e-2d4a-4b6e-9f00-112233445566"),
        RegistrationType.EXTERNAL,
        AT,
        "Žiga",
        "Čeč",
        "ziga@example.si",
        "Šola <b>",
        null,
        null,
        null,
        List.of(
            new SelectedOption("ws", "Workshop", OptionCategory.WORKSHOP),
            new SelectedOption("ev", "Event", OptionCategory.EVENT),
            new SelectedOption("ws2", "Workshop 2", OptionCategory.WORKSHOP)),
        List.of(new GivenConsent("privacy", "I agree", AT)));
  }

  public static Registration student() {
    return new Registration(
        UUID.fromString("1c9f3c1e-2d4a-4b6e-9f00-112233445566"),
        RegistrationType.STUDENT,
        AT,
        "Luka",
        "Kranjc",
        "luka@example.si",
        null,
        "Uni",
        "CS",
        "63",
        List.of(),
        List.of());
  }
}
