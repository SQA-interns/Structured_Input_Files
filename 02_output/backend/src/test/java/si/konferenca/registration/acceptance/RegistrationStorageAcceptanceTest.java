package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static si.konferenca.registration.acceptance.support.Payloads.external;
import static si.konferenca.registration.acceptance.support.Payloads.student;
import static si.konferenca.registration.acceptance.support.Payloads.with;

import com.jayway.jsonpath.DocumentContext;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;

/** US-005 Reliable registration storage. */
class RegistrationStorageAcceptanceTest extends AcceptanceTestBase {

  @Test
  @DisplayName("AC-005-01: an accepted external registration is stored with all its data")
  void ac_005_01_externalRegistrationIsStoredInDatabase() {
    Map<String, Object> submission = external();

    UUID id = registerAccepted(submission);

    Map<String, Object> row = db.registration(id).orElseThrow();
    assertThat(row.get("type")).isEqualTo("EXTERNAL");
    assertThat(row.get("first_name")).isEqualTo(submission.get("firstName"));
    assertThat(row.get("last_name")).isEqualTo(submission.get("lastName"));
    assertThat(row.get("email")).isEqualTo(submission.get("email"));
    assertThat(row.get("organization")).isEqualTo(submission.get("organization"));
    assertThat(row.get("registered_at")).isNotNull();
    assertThat(db.options(id))
        .extracting(o -> o.get("option_id"), o -> o.get("option_name"), o -> o.get("category"))
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("meal-lunch", "Kosilo", "MEAL"),
            org.assertj.core.groups.Tuple.tuple("ws-ai", "Workshop: AI v praksi", "WORKSHOP"));
    List<Map<String, Object>> consents = db.consents(id);
    assertThat(consents).hasSize(1);
    assertThat(consents.get(0).get("consent_id")).isEqualTo("privacy");
    assertThat((String) consents.get(0).get("consent_text")).isNotBlank();
    assertThat(consents.get(0).get("given_at")).isNotNull();
  }

  @Test
  @DisplayName("AC-005-01: an accepted student registration is stored with all its data")
  void ac_005_01_studentRegistrationIsStoredInDatabase() {
    Map<String, Object> submission = with(student(), "consentIds", List.of("privacy", "photo"));

    UUID id = registerAccepted(submission);

    Map<String, Object> row = db.registration(id).orElseThrow();
    assertThat(row.get("type")).isEqualTo("STUDENT");
    assertThat(row.get("first_name")).isEqualTo(submission.get("firstName"));
    assertThat(row.get("last_name")).isEqualTo(submission.get("lastName"));
    assertThat(row.get("email")).isEqualTo(submission.get("email"));
    assertThat(row.get("study_institution")).isEqualTo(submission.get("studyInstitution"));
    assertThat(row.get("study_programme")).isEqualTo(submission.get("studyProgramme"));
    assertThat(row.get("student_id")).isEqualTo(submission.get("studentId"));
    assertThat(row.get("organization")).isNull();
    assertThat(row.get("registered_at")).isNotNull();
    assertThat(db.options(id))
        .extracting(o -> o.get("option_id"))
        .containsExactly("ev-dinner", "ws-ai");
    assertThat(db.consents(id))
        .extracting(c -> c.get("consent_id"))
        .containsExactly("photo", "privacy");
  }

  @Test
  @DisplayName("AC-005-02: an accepted registration has a JSON copy named after its id")
  void ac_005_02_jsonCopyIsWritten() {
    Map<String, Object> submission = student();

    UUID id = registerAccepted(submission);

    assertThat(Files.isRegularFile(jsonCopyFile(JSON_COPY_DIR, id))).isTrue();
    DocumentContext copy = readJsonCopy(JSON_COPY_DIR, id);
    assertThat(copy.<Integer>read("$.schemaVersion")).isEqualTo(1);
    assertThat(copy.<String>read("$.id")).isEqualTo(id.toString());
    assertThat(copy.<String>read("$.type")).isEqualTo("STUDENT");
    assertThat(Instant.parse(copy.read("$.registeredAt"))).isNotNull();
    assertThat(copy.<String>read("$.participant.firstName")).isEqualTo(submission.get("firstName"));
    assertThat(copy.<String>read("$.participant.lastName")).isEqualTo(submission.get("lastName"));
    assertThat(copy.<String>read("$.participant.email")).isEqualTo(submission.get("email"));
    assertThat(copy.<String>read("$.participant.studyInstitution"))
        .isEqualTo(submission.get("studyInstitution"));
    assertThat(copy.<String>read("$.participant.studyProgramme"))
        .isEqualTo(submission.get("studyProgramme"));
    assertThat(copy.<String>read("$.participant.studentId")).isEqualTo(submission.get("studentId"));
    assertThat(copy.<List<String>>read("$.options[*].id"))
        .containsExactlyInAnyOrder("ws-ai", "ev-dinner");
    assertThat(copy.<List<String>>read("$.consents[*].id")).containsExactly("privacy");
    assertThat(copy.<List<String>>read("$.consents[*].givenAt")).hasSize(1);
    assertThat(readString(jsonCopyFile(JSON_COPY_DIR, id))).doesNotContain("captcha", "TEST-PASS");
  }

  @Test
  @DisplayName("AC-005-04: Slovenian characters are stored unchanged in database and JSON copy")
  void ac_005_04_slovenianCharactersSurviveStorage() {
    Map<String, Object> submission = external();
    String lastName = "Šuštaršič-Žagar " + submission.get("lastName");
    submission = with(submission, "firstName", "Črtomir Čeč");
    submission = with(submission, "lastName", lastName);
    submission = with(submission, "organization", "Fakulteta za računalništvo, ČŠŽ čšž ĆĐ ćđ");

    UUID id = registerAccepted(submission);

    Map<String, Object> row = db.registration(id).orElseThrow();
    assertThat(row.get("first_name")).isEqualTo("Črtomir Čeč");
    assertThat(row.get("last_name")).isEqualTo(lastName);
    assertThat(row.get("organization")).isEqualTo("Fakulteta za računalništvo, ČŠŽ čšž ĆĐ ćđ");
    DocumentContext copy = readJsonCopy(JSON_COPY_DIR, id);
    assertThat(copy.<String>read("$.participant.firstName")).isEqualTo("Črtomir Čeč");
    assertThat(copy.<String>read("$.participant.lastName")).isEqualTo(lastName);
    assertThat(copy.<String>read("$.participant.organization"))
        .isEqualTo("Fakulteta za računalništvo, ČŠŽ čšž ĆĐ ćđ");
  }
}
