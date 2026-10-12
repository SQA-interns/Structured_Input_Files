package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.TestStack;

/** US-005 Reliable registration storage: database row and raw JSON copy. */
class RegistrationStorageAcceptanceTest extends AcceptanceTestBase {

  private static final String SLOVENIAN_FIRST = "Čedomir Šime Žiga";
  private static final String SLOVENIAN_LAST = "Žagar-Ščuka čšž";
  private static final String SLOVENIAN_INSTITUTION = "Univerza v Ljubljani – Fakulteta ČŠŽ, čšž";

  @DynamicPropertySource
  static void settings(DynamicPropertyRegistry registry) {
    TestStack.register(registry);
  }

  @Test
  @DisplayName("AC-005-01 external: the database holds type, fields, options, consents with time")
  void ac_005_01_externalRegistrationIsStoredCompletely() throws Exception {
    Map<String, Object> body = validExternal();
    body.put("optionIds", new ArrayList<>(List.of("ws-testing", "ev-opening")));
    body.put("consents", new ArrayList<>(List.of("privacy", "photo")));
    Instant before = Instant.now();

    String id = register(body);
    Instant after = Instant.now();

    assertThat(registrationCount()).isEqualTo(1);
    Map<String, Object> row =
        jdbc.queryForMap(
            "SELECT type, first_name, last_name, email, organization, study_institution,"
                + " study_programme, student_id FROM registration WHERE id = ?::uuid",
            id);
    assertThat(row)
        .containsEntry("type", "EXTERNAL")
        .containsEntry("first_name", "Ana")
        .containsEntry("last_name", "Novak")
        .containsEntry("email", "ana.novak@example.org")
        .containsEntry("organization", "Institut Example")
        .containsEntry("study_institution", null)
        .containsEntry("study_programme", null)
        .containsEntry("student_id", null);
    assertWithin(registeredAt(id), before, after);
    assertThat(optionIds(id)).containsExactlyInAnyOrder("ws-testing", "ev-opening");
    assertThat(consentIds(id)).containsExactlyInAnyOrder("privacy", "photo");
    for (String consent : List.of("privacy", "photo")) {
      assertWithin(consentGivenAt(id, consent), before, after);
    }
  }

  @Test
  @DisplayName("AC-005-01 student: the database holds type, fields, options, consents with time")
  void ac_005_01_studentRegistrationIsStoredCompletely() throws Exception {
    Instant before = Instant.now();

    String id = register(validStudent());
    Instant after = Instant.now();

    Map<String, Object> row =
        jdbc.queryForMap(
            "SELECT type, first_name, last_name, email, organization, study_institution,"
                + " study_programme, student_id FROM registration WHERE id = ?::uuid",
            id);
    assertThat(row)
        .containsEntry("type", "STUDENT")
        .containsEntry("first_name", "Luka")
        .containsEntry("last_name", "Kranjc")
        .containsEntry("email", "luka.kranjc@student.example.org")
        .containsEntry("organization", null)
        .containsEntry("study_institution", "Univerza Example")
        .containsEntry("study_programme", "Computer Science")
        .containsEntry("student_id", "63200001");
    assertWithin(registeredAt(id), before, after);
    assertThat(optionIds(id)).containsExactlyInAnyOrder("ws-testing", "ev-opening");
    assertThat(consentIds(id)).containsExactly("privacy");
    assertWithin(consentGivenAt(id, "privacy"), before, after);
  }

  @Test
  @DisplayName("AC-005-02 a JSON copy named after the id contains the registration as accepted")
  void ac_005_02_jsonCopyContainsTheRegistration() throws Exception {
    Map<String, Object> body = validExternal();
    body.put("optionIds", new ArrayList<>(List.of("ws-testing", "meal-lunch")));

    String id = register(body);

    assertThat(Files.isRegularFile(jsonCopy(id))).as("file %s", jsonCopy(id)).isTrue();
    assertThat(jsonCopies()).hasSize(1);
    String copy = readJsonCopy(id);
    Map<String, Object> root = JsonPath.read(copy, "$");
    assertThat(root)
        .containsEntry("id", id)
        .containsEntry("type", "EXTERNAL")
        .containsEntry("firstName", "Ana")
        .containsEntry("lastName", "Novak")
        .containsEntry("email", "ana.novak@example.org")
        .containsEntry("organization", "Institut Example")
        .doesNotContainKeys("studyInstitution", "studyProgramme", "studentId");
    List<String> options = JsonPath.read(copy, "$.options[*].id");
    assertThat(options).containsExactlyInAnyOrder("ws-testing", "meal-lunch");
    List<String> optionNames = JsonPath.read(copy, "$.options[*].name");
    assertThat(optionNames).containsExactlyInAnyOrder("Workshop: Software testing", "Lunch");
    List<String> consents = JsonPath.read(copy, "$.consents[*].id");
    assertThat(consents).containsExactly("privacy");
    String givenAt = JsonPath.read(copy, "$.consents[0].givenAt");
    assertThat(OffsetDateTime.parse(givenAt)).isNotNull();
    String registeredAt = JsonPath.read(copy, "$.registeredAt");
    // The database keeps microseconds; the copy may be more precise.
    assertThat(Duration.between(OffsetDateTime.parse(registeredAt), registeredAt(id)).abs())
        .isLessThanOrEqualTo(Duration.ofMillis(1));
  }

  @Test
  @DisplayName("AC-005-04 Slovenian characters are unchanged in the database row and JSON copy")
  void ac_005_04_slovenianCharactersAreStoredUnchanged() throws Exception {
    Map<String, Object> external = validExternal();
    external.put("firstName", SLOVENIAN_FIRST);
    external.put("lastName", SLOVENIAN_LAST);
    external.put("organization", SLOVENIAN_INSTITUTION);
    Map<String, Object> student = validStudent();
    student.put("firstName", SLOVENIAN_FIRST);
    student.put("lastName", SLOVENIAN_LAST);
    student.put("studyInstitution", SLOVENIAN_INSTITUTION);
    student.put("email", "student.ccz@example.org");

    String externalId = register(external);
    String studentId = register(student);

    assertThat(
            jdbc.queryForMap(
                "SELECT first_name, last_name, organization FROM registration WHERE id = ?::uuid",
                externalId))
        .containsEntry("first_name", SLOVENIAN_FIRST)
        .containsEntry("last_name", SLOVENIAN_LAST)
        .containsEntry("organization", SLOVENIAN_INSTITUTION);
    assertThat(
            jdbc.queryForMap(
                "SELECT first_name, last_name, study_institution FROM registration"
                    + " WHERE id = ?::uuid",
                studentId))
        .containsEntry("first_name", SLOVENIAN_FIRST)
        .containsEntry("last_name", SLOVENIAN_LAST)
        .containsEntry("study_institution", SLOVENIAN_INSTITUTION);

    String externalCopy = readJsonCopy(externalId);
    assertThat((String) JsonPath.read(externalCopy, "$.firstName")).isEqualTo(SLOVENIAN_FIRST);
    assertThat((String) JsonPath.read(externalCopy, "$.lastName")).isEqualTo(SLOVENIAN_LAST);
    assertThat((String) JsonPath.read(externalCopy, "$.organization"))
        .isEqualTo(SLOVENIAN_INSTITUTION);
    String studentCopy = readJsonCopy(studentId);
    assertThat((String) JsonPath.read(studentCopy, "$.studyInstitution"))
        .isEqualTo(SLOVENIAN_INSTITUTION);
  }

  private OffsetDateTime registeredAt(String id) {
    return jdbc.queryForObject(
        "SELECT registered_at FROM registration WHERE id = ?::uuid", OffsetDateTime.class, id);
  }

  private OffsetDateTime consentGivenAt(String id, String consent) {
    return jdbc.queryForObject(
        "SELECT given_at FROM registration_consent WHERE registration_id = ?::uuid"
            + " AND consent_id = ?",
        OffsetDateTime.class,
        id,
        consent);
  }

  private List<String> optionIds(String id) {
    return jdbc.queryForList(
        "SELECT option_id FROM registration_option WHERE registration_id = ?::uuid",
        String.class,
        id);
  }

  private List<String> consentIds(String id) {
    return jdbc.queryForList(
        "SELECT consent_id FROM registration_consent WHERE registration_id = ?::uuid",
        String.class,
        id);
  }

  private static void assertWithin(OffsetDateTime time, Instant before, Instant after) {
    assertThat(time).isNotNull();
    // timestamptz keeps microseconds; allow one millisecond for rounding.
    assertThat(time.toInstant()).isBetween(before.minusMillis(1), after.plusMillis(1));
  }
}
