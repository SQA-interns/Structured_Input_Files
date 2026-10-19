package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.Submissions;
import tools.jackson.databind.JsonNode;

/** US-005: database row (database.sql) and JSON copy (registration-copy.schema.json). */
class StorageAcceptanceTest extends AcceptanceTestBase {

  @Test
  void AC_005_01_databaseHoldsExternalRegistrationWithFieldsOptionsConsentsAndTime()
      throws Exception {
    Map<String, Object> body = Submissions.validExternal();
    Instant before = Instant.now().minusSeconds(1);

    MvcResult result = submit(body);

    Instant after = Instant.now().plusSeconds(1);
    assertThat(result.getResponse().getStatus()).isEqualTo(201);
    UUID id = UUID.fromString(json(result).path("id").asString());
    assertThat(registrationCount()).isEqualTo(1);
    Map<String, Object> row = jdbc.queryForMap("SELECT * FROM registration WHERE id = ?", id);
    assertThat(row)
        .containsEntry("type", "EXTERNAL")
        .containsEntry("first_name", "Ana")
        .containsEntry("last_name", "Novak")
        .containsEntry("email", body.get("email"))
        .containsEntry("organization", "Institut Jozef Stefan");
    assertThat(registeredAt(id)).isBetween(before, after);
    assertThat(optionIds(id))
        .containsExactlyInAnyOrder("ws-ai", "ev-dinner", "meal-lunch", "oth-tour");
    assertConsentGiven(id, "privacy", before, after);
  }

  @Test
  void AC_005_01_databaseHoldsStudentRegistrationWithFieldsOptionsConsentsAndTime()
      throws Exception {
    Map<String, Object> body = Submissions.validStudent();
    Instant before = Instant.now().minusSeconds(1);

    MvcResult result = submit(body);

    Instant after = Instant.now().plusSeconds(1);
    assertThat(result.getResponse().getStatus()).isEqualTo(201);
    UUID id = UUID.fromString(json(result).path("id").asString());
    Map<String, Object> row = jdbc.queryForMap("SELECT * FROM registration WHERE id = ?", id);
    assertThat(row)
        .containsEntry("type", "STUDENT")
        .containsEntry("first_name", "Luka")
        .containsEntry("last_name", "Kranjc")
        .containsEntry("email", body.get("email"))
        .containsEntry("study_institution", "Univerza v Ljubljani")
        .containsEntry("study_programme", "Računalništvo in informatika")
        .containsEntry("student_id", "63210001");
    assertThat(registeredAt(id)).isBetween(before, after);
    assertThat(optionIds(id))
        .containsExactlyInAnyOrder("ws-ai", "ev-dinner", "meal-lunch", "oth-mentoring");
    assertConsentGiven(id, "privacy", before, after);
  }

  @Test
  void AC_005_02_jsonCopyNamedAfterIdContainsTheAcceptedRegistration() throws Exception {
    Map<String, Object> body = Submissions.validExternal();

    MvcResult result = submit(body);

    assertThat(result.getResponse().getStatus()).isEqualTo(201);
    String id = json(result).path("id").asString();
    Path copy = jsonCopyDir().resolve(id + ".json");
    assertThat(copy).isRegularFile();
    JsonNode stored = JSON.readTree(Files.readString(copy, StandardCharsets.UTF_8));
    assertThat(stored.path("id").asString()).isEqualTo(id);
    assertThat(stored.path("type").asString()).isEqualTo("EXTERNAL");
    assertThat(stored.path("firstName").asString()).isEqualTo("Ana");
    assertThat(stored.path("lastName").asString()).isEqualTo("Novak");
    assertThat(stored.path("email").asString()).isEqualTo(body.get("email"));
    assertThat(stored.path("organization").asString()).isEqualTo("Institut Jozef Stefan");
    assertThat(Instant.parse(stored.path("registeredAt").asString())).isNotNull();
    List<String> options = new ArrayList<>();
    stored.path("options").forEach(o -> options.add(o.path("id").asString()));
    assertThat(options).containsExactlyInAnyOrder("ws-ai", "ev-dinner", "meal-lunch", "oth-tour");
    List<String> consents = new ArrayList<>();
    stored.path("consents").forEach(c -> consents.add(c.path("id").asString()));
    assertThat(consents).containsExactly("privacy");
  }

  @Test
  void AC_005_03_unwritableJsonCopyFailsTheRequestAndLeavesNoDatabaseRow() throws Exception {
    Path dir = jsonCopyDir();
    clearDirectory(dir);
    Files.delete(dir);
    Files.writeString(dir, "a regular file where the JSON copy directory should be");
    try {
      MvcResult result = submit(Submissions.validExternal());

      assertThat(result.getResponse().getStatus()).isNotEqualTo(201).isEqualTo(500);
      assertThat(registrationCount()).isZero();
    } finally {
      clearDirectory(dir);
    }
  }

  @Test
  void AC_005_04_slovenianCharactersAreStoredUnchangedInDatabaseAndJsonCopy() throws Exception {
    Map<String, Object> body = Submissions.validExternal();
    body.put("firstName", "Žiga Čedomir");
    body.put("lastName", "Šuštaršič-Žužek");
    body.put("organization", "Fakulteta za računalništvo, ČŠŽ čšž");

    MvcResult result = submit(body);

    assertThat(result.getResponse().getStatus()).isEqualTo(201);
    String id = json(result).path("id").asString();
    Map<String, Object> row =
        jdbc.queryForMap("SELECT * FROM registration WHERE id = ?", UUID.fromString(id));
    assertThat(row)
        .containsEntry("first_name", "Žiga Čedomir")
        .containsEntry("last_name", "Šuštaršič-Žužek")
        .containsEntry("organization", "Fakulteta za računalništvo, ČŠŽ čšž");
    JsonNode stored =
        JSON.readTree(
            Files.readString(jsonCopyDir().resolve(id + ".json"), StandardCharsets.UTF_8));
    assertThat(stored.path("firstName").asString()).isEqualTo("Žiga Čedomir");
    assertThat(stored.path("lastName").asString()).isEqualTo("Šuštaršič-Žužek");
    assertThat(stored.path("organization").asString())
        .isEqualTo("Fakulteta za računalništvo, ČŠŽ čšž");
  }

  private Instant registeredAt(UUID id) {
    return jdbc.queryForObject(
            "SELECT registered_at FROM registration WHERE id = ?",
            (rs, n) -> rs.getObject("registered_at", OffsetDateTime.class),
            id)
        .toInstant();
  }

  private List<String> optionIds(UUID id) {
    return jdbc.queryForList(
        "SELECT option_id FROM registration_option WHERE registration_id = ?", String.class, id);
  }

  private void assertConsentGiven(UUID id, String consentId, Instant before, Instant after) {
    List<OffsetDateTime> givenAt =
        jdbc.query(
            "SELECT given_at FROM registration_consent WHERE registration_id = ? AND consent_id = ?",
            (rs, n) -> rs.getObject("given_at", OffsetDateTime.class),
            id,
            consentId);
    assertThat(givenAt).hasSize(1);
    assertThat(givenAt.get(0).toInstant()).isBetween(before, after);
  }
}
