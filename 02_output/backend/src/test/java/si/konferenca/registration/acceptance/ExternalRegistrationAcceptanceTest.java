package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** US-001 external participant registration, through the REST API. */
class ExternalRegistrationAcceptanceTest extends AcceptanceTestBase {

  private static final List<String> REQUIRED =
      List.of("firstName", "lastName", "email", "organization");

  static Stream<Arguments> requiredFieldVariants() {
    return REQUIRED.stream()
        .flatMap(
            f ->
                Stream.of(Arguments.of(f, "missing"), Arguments.of(f, ""), Arguments.of(f, "   ")));
  }

  @Test
  @DisplayName("AC-001-01 valid external submission is accepted with a new registration id")
  void ac00101ValidExternalSubmissionAccepted() {
    Api.Response response = api.postRegistration(Submissions.validExternal());

    String id = assertAccepted(response);
    assertThat(AcceptanceEnvironment.query("SELECT id FROM registration WHERE id = ?::uuid", id))
        .hasSize(1);
  }

  @ParameterizedTest(name = "AC-001-02 {0} = [{1}] is rejected")
  @MethodSource("requiredFieldVariants")
  @DisplayName("AC-001-02 missing, empty or whitespace required external field is rejected")
  void ac00102RequiredFieldRejected(String field, String value) {
    ObjectNode body = Submissions.validExternal();
    if ("missing".equals(value)) {
      body.remove(field);
    } else {
      body.put(field, value);
    }

    assertRejected(api.postRegistration(body), field);
  }

  @Test
  @DisplayName("AC-001-03 leading and trailing spaces are removed from stored values")
  void ac00103StoredValuesTrimmed() {
    ObjectNode body = Submissions.validExternal();
    body.put("firstName", "  Ana ");
    body.put("lastName", "\tNovak  ");
    body.put("email", "  ana.novak@example.si ");
    body.put("organization", "   Institut Primer   ");

    String id = assertAccepted(api.postRegistration(body));

    Map<String, Object> row =
        AcceptanceEnvironment.query(
                "SELECT first_name, last_name, email, organization FROM registration"
                    + " WHERE id = ?::uuid",
                id)
            .get(0);
    assertThat(row)
        .containsEntry("first_name", "Ana")
        .containsEntry("last_name", "Novak")
        .containsEntry("email", "ana.novak@example.si")
        .containsEntry("organization", "Institut Primer");
    JsonNode copy =
        AcceptanceEnvironment.JSON.readTree(AcceptanceEnvironment.jsonCopy(id).toFile());
    assertThat(copy.get("firstName").asString()).isEqualTo("Ana");
    assertThat(copy.get("lastName").asString()).isEqualTo("Novak");
    assertThat(copy.get("email").asString()).isEqualTo("ana.novak@example.si");
    assertThat(copy.get("organization").asString()).isEqualTo("Institut Primer");
  }

  @ParameterizedTest(name = "AC-001-04 email [{0}] is rejected")
  @ValueSource(
      strings = {
        "ana.novak",
        "ana.novak@",
        "@example.si",
        "ana novak@example.si",
        "ana@example",
        "ana@@example.si"
      })
  @DisplayName("AC-001-04 invalid email format is rejected")
  void ac00104InvalidEmailRejected(String email) {
    ObjectNode body = Submissions.validExternal();
    body.put("email", email);

    assertRejected(api.postRegistration(body), "email");
  }

  @ParameterizedTest(name = "AC-001-05 option ids {0} are rejected")
  @ValueSource(strings = {"no-such-option", "ws-off", "ws-a,ws-a"})
  @DisplayName("AC-001-05 unknown, inactive or duplicate option is rejected")
  void ac00105InvalidOptionRejected(String ids) {
    ObjectNode body = Submissions.validExternal();
    var options = body.putArray("optionIds");
    for (String id : ids.split(",")) {
      options.add(id);
    }

    assertRejected(api.postRegistration(body), "options");
  }

  @ParameterizedTest(name = "AC-001-06 privacy consent {0} is rejected")
  @ValueSource(strings = {"false", "absent", "no consents object"})
  @DisplayName("AC-001-06 mandatory consent not given is rejected")
  void ac00106MandatoryConsentMissingRejected(String variant) {
    ObjectNode body = Submissions.validExternal();
    switch (variant) {
      case "false" -> body.putObject("consents").put("privacy", false);
      case "absent" -> body.putObject("consents").put("photos", true);
      default -> body.remove("consents");
    }

    assertRejected(api.postRegistration(body), "consents.privacy");
  }

  @ParameterizedTest(name = "AC-001-07 reCAPTCHA token {0} is rejected")
  @ValueSource(strings = {"missing", "empty", "rejected-by-verifier"})
  @DisplayName("AC-001-07 missing or rejected reCAPTCHA token is rejected and nothing stored")
  void ac00107CaptchaRejected(String variant) {
    ObjectNode body = Submissions.validExternal();
    switch (variant) {
      case "missing" -> body.remove("recaptchaToken");
      case "empty" -> body.put("recaptchaToken", "");
      default -> body.put("recaptchaToken", "rejected-token");
    }

    assertRejected(api.postRegistration(body), "recaptchaToken");
  }
}
