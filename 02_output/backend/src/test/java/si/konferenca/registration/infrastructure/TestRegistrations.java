package si.konferenca.registration.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import si.konferenca.registration.domain.Category;
import si.konferenca.registration.domain.ConsentRecord;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;
import si.konferenca.registration.domain.SelectedOption;

/** Sample accepted registrations for adapter tests. */
public final class TestRegistrations {

  public static final Instant AT = Instant.parse("2026-10-12T10:00:00Z");

  private TestRegistrations() {}

  public static Registration external(String firstName, List<SelectedOption> options) {
    return new Registration(
        UUID.fromString("11111111-1111-1111-1111-111111111111"),
        RegistrationType.EXTERNAL,
        AT,
        firstName,
        "Novak",
        "ana@example.si",
        "Acme",
        null,
        null,
        null,
        options,
        new ConsentRecord("c", "I agree.", AT));
  }

  public static Registration student() {
    return new Registration(
        UUID.fromString("22222222-2222-2222-2222-222222222222"),
        RegistrationType.STUDENT,
        AT.plusSeconds(60),
        "Luka",
        "Kranjc",
        "luka@example.si",
        null,
        "Uni",
        "CS",
        "123",
        List.of(
            new SelectedOption("fair", "Career fair", Category.OTHER),
            new SelectedOption("ws", "Workshop", Category.WORKSHOP),
            new SelectedOption("ws2", "Workshop 2", Category.WORKSHOP)),
        new ConsentRecord("c", "I agree.", AT.plusSeconds(60)));
  }
}
