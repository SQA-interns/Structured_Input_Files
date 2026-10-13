package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** US-005 reliable storage: database row and raw JSON copy of every accepted registration. */
class StorageAcceptanceTest extends AcceptanceTestBase {

  @Test
  @DisplayName(
      "AC-005-01 external registration is stored with type, fields, options, consents and times")
  void ac00501ExternalStored() {
    Instant before = Instant.now().minusSeconds(1);
    String id = assertAccepted(api.postRegistration(Submissions.validExternal()));
    Instant after = Instant.now().plusSeconds(1);

    Map<String, Object> row = registration(id);
    assertThat(row)
        .containsEntry("type", "EXTERNAL")
        .containsEntry("first_name", "Ana")
        .containsEntry("last_name", "Novak")
        .containsEntry("email", "ana.novak@example.si")
        .containsEntry("organization", "Institut Primer");
    assertThat(instant(row.get("registered_at"))).isBetween(before, after);
    assertThat(optionIds(id)).containsExactlyInAnyOrder("ws-a", "ws-ext", "ev-a", "meal-a", "ot-a");
    assertConsentGiven(id, "privacy", before, after);
  }

  @Test
  @DisplayName(
      "AC-005-01 student registration is stored with type, fields, options, consents and times")
  void ac00501StudentStored() {
    Instant before = Instant.now().minusSeconds(1);
    String id = assertAccepted(api.postRegistration(Submissions.validStudent()));
    Instant after = Instant.now().plusSeconds(1);

    Map<String, Object> row = registration(id);
    assertThat(row)
        .containsEntry("type", "STUDENT")
        .containsEntry("first_name", "Luka")
        .containsEntry("last_name", "Kranjc")
        .containsEntry("email", "luka.kranjc@student.example.si")
        .containsEntry("study_institution", "Univerza v Ljubljani")
        .containsEntry("study_programme", "Računalništvo in informatika")
        .containsEntry("student_id", "63210001");
    assertThat(instant(row.get("registered_at"))).isBetween(before, after);
    assertThat(optionIds(id)).containsExactlyInAnyOrder("ws-a", "meal-a", "ot-stu");
    assertConsentGiven(id, "privacy", before, after);
  }

  @Test
  @DisplayName(
      "AC-005-02 a JSON copy named after the registration id holds the accepted registration")
  void ac00502JsonCopyWritten() throws Exception {
    String id = assertAccepted(api.postRegistration(Submissions.validExternal()));

    assertThat(AcceptanceEnvironment.jsonCopy(id)).exists();
    JsonNode copy =
        AcceptanceEnvironment.JSON.readTree(Files.readString(AcceptanceEnvironment.jsonCopy(id)));
    assertThat(copy.get("id").asString()).isEqualTo(id);
    assertThat(copy.get("type").asString()).isEqualTo("EXTERNAL");
    assertThat(copy.get("firstName").asString()).isEqualTo("Ana");
    assertThat(copy.get("lastName").asString()).isEqualTo("Novak");
    assertThat(copy.get("email").asString()).isEqualTo("ana.novak@example.si");
    assertThat(copy.get("organization").asString()).isEqualTo("Institut Primer");
    assertThat(OffsetDateTime.parse(copy.get("registeredAt").asString()).toInstant())
        .isEqualTo(instant(registration(id).get("registered_at")).truncatedTo(ChronoUnit.MILLIS));
    List<String> options = new ArrayList<>();
    copy.get("options").forEach(o -> options.add(o.get("id").asString()));
    assertThat(options).containsExactlyInAnyOrder("ws-a", "ws-ext", "ev-a", "meal-a", "ot-a");
    List<String> consents = new ArrayList<>();
    copy.get("consents").forEach(c -> consents.add(c.get("id").asString()));
    assertThat(consents).contains("privacy");
  }

  @Test
  @DisplayName(
      "AC-005-04 Slovenian characters are stored unchanged in the database and the JSON copy")
  void ac00504SlovenianCharactersUnchanged() throws Exception {
    ObjectNode body = Submissions.validExternal();
    body.put("firstName", "Žiga Čeh");
    body.put("lastName", "Šuštaršič ČŠŽ");
    body.put("organization", "Fakulteta za računalništvo, čšž ČŠŽ");

    String id = assertAccepted(api.postRegistration(body));

    assertThat(registration(id))
        .containsEntry("first_name", "Žiga Čeh")
        .containsEntry("last_name", "Šuštaršič ČŠŽ")
        .containsEntry("organization", "Fakulteta za računalništvo, čšž ČŠŽ");
    JsonNode copy =
        AcceptanceEnvironment.JSON.readTree(Files.readString(AcceptanceEnvironment.jsonCopy(id)));
    assertThat(copy.get("firstName").asString()).isEqualTo("Žiga Čeh");
    assertThat(copy.get("lastName").asString()).isEqualTo("Šuštaršič ČŠŽ");
    assertThat(copy.get("organization").asString())
        .isEqualTo("Fakulteta za računalništvo, čšž ČŠŽ");
  }

  private static Map<String, Object> registration(String id) {
    List<Map<String, Object>> rows =
        AcceptanceEnvironment.query("SELECT * FROM registration WHERE id = ?::uuid", id);
    assertThat(rows).as("registration rows for %s", id).hasSize(1);
    return rows.get(0);
  }

  private static List<String> optionIds(String id) {
    List<String> ids = new ArrayList<>();
    AcceptanceEnvironment.query(
            "SELECT option_id FROM registration_option WHERE registration_id = ?::uuid", id)
        .forEach(r -> ids.add((String) r.get("option_id")));
    return ids;
  }

  private static void assertConsentGiven(String id, String consent, Instant before, Instant after) {
    List<Map<String, Object>> rows =
        AcceptanceEnvironment.query(
            "SELECT given_at FROM registration_consent WHERE registration_id = ?::uuid"
                + " AND consent_id = ?",
            id,
            consent);
    assertThat(rows).as("consent %s", consent).hasSize(1);
    assertThat(instant(rows.get(0).get("given_at"))).isBetween(before, after);
  }

  /** JDBC returns java.sql.Timestamp for timestamptz unless another type is requested. */
  static Instant instant(Object value) {
    if (value instanceof Timestamp t) {
      return t.toInstant();
    }
    if (value instanceof OffsetDateTime o) {
      return o.toInstant();
    }
    throw new IllegalArgumentException("not a timestamp: " + value);
  }
}
