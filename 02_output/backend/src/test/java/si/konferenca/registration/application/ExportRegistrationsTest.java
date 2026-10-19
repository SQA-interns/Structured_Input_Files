package si.konferenca.registration.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;

class ExportRegistrationsTest {

  static Registration at(String time) {
    return new Registration(
        UUID.randomUUID(),
        RegistrationType.EXTERNAL,
        Instant.parse(time),
        "A",
        "B",
        "a@example.com",
        "O",
        null,
        null,
        null,
        List.of(),
        List.of());
  }

  @Test
  void AC_008_01_exportWritesRegistrationsOrderedByRegistrationTime() {
    Registration late = at("2026-10-19T12:00:00Z");
    Registration early = at("2026-10-19T09:00:00Z");
    Registration middle = at("2026-10-19T10:30:00Z");
    List<List<Registration>> written = new ArrayList<>();

    byte[] result =
        new ExportRegistrations(
                () -> List.of(late, early, middle),
                registrations -> {
                  written.add(registrations);
                  return new byte[] {1, 2, 3};
                })
            .export();

    assertThat(result).containsExactly(1, 2, 3);
    assertThat(written).hasSize(1);
    assertThat(written.get(0)).containsExactly(early, middle, late);
  }
}
