package si.konferenca.registration.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import si.konferenca.registration.acceptance.support.AcceptanceEnvironment;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.application.RegistrationReader;
import si.konferenca.registration.application.RegistrationStore;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;

/**
 * The JPA store against PostgreSQL with the Flyway schema (database.sql, AR-06).
 *
 * <p>Covers AC-001-01, AC-005-01, AC-005-04, AC-008-01.
 */
@SpringBootTest
class JpaRegistrationStoreIntegrationTest {

  @Autowired RegistrationStore store;
  @Autowired RegistrationReader reader;

  private final JdbcTemplate jdbc = AcceptanceEnvironment.jdbc();

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    AcceptanceEnvironment.register(registry, Map.of());
  }

  @BeforeEach
  void clean() {
    AcceptanceTestBase.clearStorage(jdbc);
  }

  @Test
  void AC_001_01_savedRegistrationIsOneRowWithOptionsAndConsents() {
    Instant time = Instant.parse("2026-10-19T12:00:00.123Z");
    UUID id = UUID.randomUUID();
    Registration registration =
        new Registration(
            id,
            RegistrationType.EXTERNAL,
            time,
            "Žiga",
            "Novak",
            "ziga@example.com",
            "IJS Čšž",
            null,
            null,
            null,
            List.of(
                new ConferenceOption("ws-a", "A", OptionCategory.WORKSHOP, true, null),
                new ConferenceOption("ev-b", "B", OptionCategory.EVENT, true, null)),
            List.of(new GivenConsent("privacy", time)));

    store.save(registration);

    Map<String, Object> row = jdbc.queryForMap("SELECT * FROM registration WHERE id = ?", id);
    assertThat(row)
        .containsEntry("type", "EXTERNAL")
        .containsEntry("first_name", "Žiga")
        .containsEntry("last_name", "Novak")
        .containsEntry("email", "ziga@example.com")
        .containsEntry("organization", "IJS Čšž")
        .containsEntry("study_institution", null)
        .containsEntry("study_programme", null)
        .containsEntry("student_id", null);
    OffsetDateTime registeredAt =
        jdbc.queryForObject(
            "SELECT registered_at FROM registration WHERE id = ?",
            (rs, n) -> rs.getObject(1, OffsetDateTime.class),
            id);
    assertThat(registeredAt.toInstant()).isEqualTo(time);
    assertThat(
            jdbc.queryForList(
                "SELECT option_id FROM registration_option WHERE registration_id = ?",
                String.class,
                id))
        .containsExactlyInAnyOrder("ws-a", "ev-b");
    List<Map<String, Object>> consents =
        jdbc.queryForList(
            "SELECT consent_id FROM registration_consent WHERE registration_id = ?", id);
    assertThat(consents).extracting(m -> m.get("consent_id")).containsExactly("privacy");
  }

  @Test
  void AC_001_01_schemaMatchesTheStorageContract() {
    List<String> columns =
        jdbc.queryForList(
            "SELECT column_name FROM information_schema.columns WHERE table_name = 'registration'",
            String.class);

    assertThat(columns)
        .containsExactlyInAnyOrder(
            "id",
            "type",
            "first_name",
            "last_name",
            "email",
            "organization",
            "study_institution",
            "study_programme",
            "student_id",
            "registered_at");
  }

  @Test
  void AC_008_01_storedRegistrationsAreReadBackWithConfiguredOptionNames() {
    Instant time = Instant.parse("2026-10-19T12:00:00.123Z");
    UUID id = UUID.randomUUID();
    store.save(
        new Registration(
            id,
            RegistrationType.STUDENT,
            time,
            "Luka",
            "Kranjc",
            "luka@example.com",
            null,
            "Univerza",
            "Računalništvo",
            "6321",
            List.of(
                new ConferenceOption("meal-lunch", "x", OptionCategory.MEAL, true, null),
                new ConferenceOption("ws-ai", "x", OptionCategory.WORKSHOP, true, null),
                new ConferenceOption("gone", "x", OptionCategory.OTHER, true, null)),
            List.of(new GivenConsent("privacy", time))));

    List<Registration> all = reader.findAll();

    assertThat(all).hasSize(1);
    Registration read = all.get(0);
    assertThat(read.id()).isEqualTo(id);
    assertThat(read.type()).isEqualTo(RegistrationType.STUDENT);
    assertThat(read.registeredAt()).isEqualTo(time);
    assertThat(read.firstName()).isEqualTo("Luka");
    assertThat(read.lastName()).isEqualTo("Kranjc");
    assertThat(read.email()).isEqualTo("luka@example.com");
    assertThat(read.organization()).isNull();
    assertThat(read.studyInstitution()).isEqualTo("Univerza");
    assertThat(read.studyProgramme()).isEqualTo("Računalništvo");
    assertThat(read.studentId()).isEqualTo("6321");
    assertThat(read.options())
        .extracting(ConferenceOption::id)
        .containsExactly("ws-ai", "meal-lunch", "gone");
    assertThat(read.options().get(0).name()).isEqualTo("Workshop: AI in practice");
    assertThat(read.options().get(2).name()).isEqualTo("gone");
    assertThat(read.consents()).containsExactly(new GivenConsent("privacy", time));
  }
}
