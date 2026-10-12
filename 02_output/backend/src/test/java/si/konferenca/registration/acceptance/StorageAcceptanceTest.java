package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.acceptance.support.DefaultContextAcceptanceTest;
import si.konferenca.registration.acceptance.support.Payloads;
import tools.jackson.databind.JsonNode;

/** US-005 Reliable registration storage (AC-005-01, AC-005-02, AC-005-04). */
class StorageAcceptanceTest extends DefaultContextAcceptanceTest {

  private static final String PRIVACY_TEXT =
      "I agree that my personal data is processed for organizing the conference and that it is"
          + " shared with the conference organizers.";

  private static Map<String, Object> row(String id) {
    return DB.query(
            "SELECT registration_type, first_name, last_name, email, organization,"
                + " study_institution, study_programme, student_id, registered_at"
                + " FROM registration WHERE id = ?::uuid",
            (rs, n) -> {
              Map<String, Object> r = new java.util.HashMap<>();
              for (String c :
                  List.of(
                      "registration_type",
                      "first_name",
                      "last_name",
                      "email",
                      "organization",
                      "study_institution",
                      "study_programme",
                      "student_id")) {
                r.put(c, rs.getString(c));
              }
              r.put("registered_at", rs.getObject("registered_at", OffsetDateTime.class));
              return r;
            },
            id)
        .getFirst();
  }

  private static List<Map<String, Object>> optionRows(String id) {
    return DB.queryForList(
        "SELECT option_id, option_name, option_category FROM registration_option"
            + " WHERE registration_id = ?::uuid ORDER BY option_id",
        id);
  }

  private static List<Map<String, Object>> consentRows(String id) {
    return DB.query(
        "SELECT consent_id, consent_text, given_at FROM registration_consent"
            + " WHERE registration_id = ?::uuid",
        (rs, n) ->
            Map.of(
                "consent_id", rs.getString("consent_id"),
                "consent_text", rs.getString("consent_text"),
                "given_at", rs.getObject("given_at", OffsetDateTime.class)),
        id);
  }

  @Test
  void AC_005_01_externalRegistrationIsStoredWithEveryFieldOptionsConsentsAndTime() {
    String email = Payloads.uniqueEmail("ac00501e");
    Instant before = Instant.now().minusSeconds(1);

    HttpResponse<String> response = postRegistration(Payloads.external(email));

    Instant after = Instant.now().plusSeconds(1);
    assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
    JsonNode created = json(response.body());
    String id = created.path("id").asString();
    Map<String, Object> row = row(id);
    assertThat(row)
        .containsEntry("registration_type", "EXTERNAL")
        .containsEntry("first_name", "Ana")
        .containsEntry("last_name", "Novak")
        .containsEntry("email", email)
        .containsEntry("organization", "Acme Testing Ltd");
    assertThat(row.get("study_institution")).isNull();
    assertThat(row.get("student_id")).isNull();
    Instant registeredAt = ((OffsetDateTime) row.get("registered_at")).toInstant();
    assertThat(registeredAt).isBetween(before, after);
    assertThat(registeredAt)
        .isCloseTo(
            Instant.parse(created.path("registeredAt").asString()), within(1, ChronoUnit.MILLIS));

    assertThat(optionRows(id))
        .containsExactly(
            Map.of(
                "option_id",
                "ev-opening",
                "option_name",
                "Opening reception",
                "option_category",
                "EVENT"),
            Map.of(
                "option_id", "ws-security",
                "option_name", "Secure coding workshop",
                "option_category", "WORKSHOP"));

    List<Map<String, Object>> consents = consentRows(id);
    assertThat(consents).hasSize(1);
    assertThat(consents.getFirst())
        .containsEntry("consent_id", "privacy")
        .containsEntry("consent_text", PRIVACY_TEXT);
    Instant givenAt = ((OffsetDateTime) consents.getFirst().get("given_at")).toInstant();
    assertThat(givenAt).isBetween(before, after);
  }

  @Test
  void AC_005_01_studentRegistrationIsStoredWithEveryField() {
    String email = Payloads.uniqueEmail("ac00501s");
    Map<String, Object> body = Payloads.student(email);
    body.put("consents", List.of("privacy", "photos"));

    String id = registerOk(body);

    Map<String, Object> row = row(id);
    assertThat(row)
        .containsEntry("registration_type", "STUDENT")
        .containsEntry("first_name", "Bor")
        .containsEntry("last_name", "Kranjc")
        .containsEntry("email", email)
        .containsEntry("study_institution", "University of Maribor")
        .containsEntry("study_programme", "Computer Science")
        .containsEntry("student_id", "63210001");
    assertThat(row.get("organization")).isNull();
    assertThat(row.get("registered_at")).isNotNull();
    assertThat(optionRows(id))
        .extracting(r -> r.get("option_id"))
        .containsExactly("meal-lunch", "ws-testing");
    assertThat(consentRows(id))
        .extracting(r -> r.get("consent_id"))
        .containsExactlyInAnyOrder("privacy", "photos");
  }

  @Test
  void AC_005_02_jsonCopyNamedAfterIdContainsTheAcceptedRegistration() throws Exception {
    String email = Payloads.uniqueEmail("ac00502");
    Map<String, Object> body = Payloads.external("  " + email);
    body.put("firstName", " Ana ");

    HttpResponse<String> response = postRegistration(body);

    assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
    JsonNode created = json(response.body());
    String id = created.path("id").asString();
    Path copy = copyFile(id);
    assertThat(copy).exists();
    JsonNode stored = JSON.readTree(Files.readAllBytes(copy));
    assertThat(stored.path("id").asString()).isEqualTo(id);
    assertThat(stored.path("type").asString()).isEqualTo("EXTERNAL");
    assertThat(stored.path("firstName").asString()).isEqualTo("Ana");
    assertThat(stored.path("lastName").asString()).isEqualTo("Novak");
    assertThat(stored.path("email").asString()).isEqualTo(email);
    assertThat(stored.path("organization").asString()).isEqualTo("Acme Testing Ltd");
    assertThat(Instant.parse(stored.path("registeredAt").asString()))
        .isEqualTo(Instant.parse(created.path("registeredAt").asString()));
    assertThat(stored.path("options").valueStream().map(o -> o.path("id").asString()).toList())
        .containsExactlyInAnyOrder("ws-security", "ev-opening");
    assertThat(stored.path("consents").valueStream().map(c -> c.path("id").asString()).toList())
        .containsExactly("privacy");
    assertThat(stored.path("consents").get(0).path("givenAt").asString()).isNotBlank();
  }

  @Test
  void AC_005_04_slovenianCharactersAreStoredUnchangedInDatabaseAndJsonCopy() throws Exception {
    String email = Payloads.uniqueEmail("ac00504");
    Map<String, Object> external = Payloads.external(email);
    external.put("firstName", "Žiga Čedomir");
    external.put("lastName", "Šuštaršič");
    external.put("organization", "Univerza v Ljubljani, Fakulteta za računalništvo (ČŠŽ čšž)");

    String id = registerOk(external);

    Map<String, Object> row = row(id);
    assertThat(row)
        .containsEntry("first_name", "Žiga Čedomir")
        .containsEntry("last_name", "Šuštaršič")
        .containsEntry(
            "organization", "Univerza v Ljubljani, Fakulteta za računalništvo (ČŠŽ čšž)");
    String raw = Files.readString(copyFile(id), java.nio.charset.StandardCharsets.UTF_8);
    assertThat(raw).contains("Žiga Čedomir", "Šuštaršič", "računalništvo (ČŠŽ čšž)");
    JsonNode stored = JSON.readTree(raw);
    assertThat(stored.path("lastName").asString()).isEqualTo("Šuštaršič");
  }

  @Test
  void AC_005_04_slovenianCharactersInStudentInstitutionAreStoredUnchanged() throws Exception {
    String email = Payloads.uniqueEmail("ac00504s");
    Map<String, Object> student = Payloads.student(email);
    student.put("firstName", "Špela");
    student.put("lastName", "Žnidaršič");
    student.put("studyInstitution", "Šolski center Celje, Višja strokovna šola (ČŠŽ)");

    String id = registerOk(student);

    assertThat(row(id))
        .containsEntry("first_name", "Špela")
        .containsEntry("last_name", "Žnidaršič")
        .containsEntry("study_institution", "Šolski center Celje, Višja strokovna šola (ČŠŽ)");
    JsonNode stored = JSON.readTree(Files.readAllBytes(copyFile(id)));
    assertThat(stored.path("studyInstitution").asString())
        .isEqualTo("Šolski center Celje, Višja strokovna šola (ČŠŽ)");
    assertThat(stored.path("lastName").asString()).isEqualTo("Žnidaršič");
  }
}
