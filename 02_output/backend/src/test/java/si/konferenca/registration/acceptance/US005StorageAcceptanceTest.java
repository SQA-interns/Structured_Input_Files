package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** US-005 reliable storage: database row and raw JSON copy (BR-07, AR-05). */
class US005StorageAcceptanceTest extends AcceptanceTestBase {

  private static final String CONSENT_TEXT =
      "I agree to the processing of my personal data for organising the conference.";

  @Test
  @DisplayName("AC-005-01 an accepted registration is stored with every field, options and consent")
  void ac005_01_acceptedRegistrationIsStoredInTheDatabase() throws Exception {
    Map<String, Object> request = Registrations.validExternal();

    Response response = api.postRegistration(request);

    assertThat(response.status()).as(response.toString()).isEqualTo(201);
    String id = response.json("$.id");
    Map<String, Object> row = db.registration(id);
    assertThat(row).isNotNull();
    assertThat(row.get("type")).isEqualTo("EXTERNAL");
    assertThat(row.get("first_name")).isEqualTo("Ana");
    assertThat(row.get("last_name")).isEqualTo("Novak");
    assertThat(row.get("email")).isEqualTo(request.get("email"));
    assertThat(row.get("organization")).isEqualTo("Acme d.o.o.");
    assertThat(row.get("consent_id")).isEqualTo("data-processing");
    assertThat(row.get("consent_text")).isEqualTo(CONSENT_TEXT);
    assertThat(row.get("consent_given_at")).isNotNull();
    assertThat(row.get("submitted_at")).isNotNull();
    assertThat(db.optionIds(id)).containsExactly("ws-a", "meal-lunch");
  }

  @Test
  @DisplayName("AC-005-02 an accepted registration has its JSON copy named after its id")
  void ac005_02_acceptedRegistrationHasItsJsonCopy() throws Exception {
    Map<String, Object> request = Registrations.validStudent();

    Response response = api.postRegistration(request);

    assertThat(response.status()).as(response.toString()).isEqualTo(201);
    String id = response.json("$.id");
    Path copy = copies.copyOf(id);
    assertThat(copy).exists();
    String json = Files.readString(copy, StandardCharsets.UTF_8);
    assertThat((String) JsonPath.read(json, "$.id")).isEqualTo(id);
    assertThat((String) JsonPath.read(json, "$.type")).isEqualTo("STUDENT");
    assertThat((String) JsonPath.read(json, "$.firstName")).isEqualTo("Luka");
    assertThat((String) JsonPath.read(json, "$.lastName")).isEqualTo("Kranjc");
    assertThat((String) JsonPath.read(json, "$.email")).isEqualTo(request.get("email"));
    assertThat((String) JsonPath.read(json, "$.studyInstitution"))
        .isEqualTo("University of Ljubljana");
    assertThat((String) JsonPath.read(json, "$.studyProgramme")).isEqualTo("Computer Science");
    assertThat((String) JsonPath.read(json, "$.studentId")).isEqualTo("63210001");
    assertThat((List<String>) JsonPath.read(json, "$.selectedOptions[*].id"))
        .containsExactly("ws-b", "other-fair");
    assertThat((String) JsonPath.read(json, "$.consent.id")).isEqualTo("data-processing");
    assertThat((String) JsonPath.read(json, "$.consent.givenAt")).isNotBlank();
    assertThat((String) JsonPath.read(json, "$.submittedAt")).isNotBlank();
    assertThat(json).doesNotContain("recaptchaToken").doesNotContain(Registrations.TEST_MODE_TOKEN);
  }

  @Test
  @DisplayName("AC-005-03 when the JSON copy cannot be written nothing is stored or sent")
  void ac005_03_copyFailureRejectsTheRegistration() throws Exception {
    copies.makeReadOnly();
    try {
      Response response = api.postRegistration(Registrations.validExternal());

      assertThat(response.status()).as(response.toString()).isEqualTo(500);
    } finally {
      copies.makeWritable();
    }
    assertNothingStoredCopiedOrSent();
  }
}
