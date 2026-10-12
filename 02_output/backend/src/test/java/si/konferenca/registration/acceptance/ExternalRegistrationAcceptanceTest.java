package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.TestStack;

/** US-001 External participant registration, through POST /api/registrations. */
class ExternalRegistrationAcceptanceTest extends AcceptanceTestBase {

  @DynamicPropertySource
  static void settings(DynamicPropertyRegistry registry) {
    TestStack.register(registry);
  }

  @Test
  @DisplayName("AC-001-01 valid external submission is accepted with 201 and the new id")
  void ac_001_01_validExternalSubmissionIsAccepted() throws Exception {
    MvcResult result = submit(validExternal());

    assertThat(result.getResponse().getStatus()).as(body(result)).isEqualTo(201);
    String id = JsonPath.read(body(result), "$.id");
    assertThat(id).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM registration WHERE id = ?::uuid", Integer.class, id))
        .isEqualTo(1);
  }

  @Test
  @DisplayName("AC-001-01 valid external submission without options is accepted")
  void ac_001_01_validExternalSubmissionWithoutOptionsIsAccepted() throws Exception {
    Map<String, Object> body = validExternal();
    body.put("optionIds", new ArrayList<>());

    String id = register(body);

    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM registration WHERE id = ?::uuid", Integer.class, id))
        .isEqualTo(1);
  }

  static Stream<Arguments> requiredFieldVariants() {
    List<String> fields = List.of("firstName", "lastName", "email", "organization");
    List<String> variants = List.of("missing", "empty", "whitespace");
    return fields.stream().flatMap(f -> variants.stream().map(v -> Arguments.of(f, v)));
  }

  @ParameterizedTest(name = "AC-001-02 {0} {1} is rejected")
  @MethodSource("requiredFieldVariants")
  void ac_001_02_missingRequiredFieldIsRejected(String field, String variant) throws Exception {
    Map<String, Object> body = validExternal();
    switch (variant) {
      case "missing" -> body.remove(field);
      case "empty" -> body.put(field, "");
      default -> body.put(field, " \t  ");
    }

    assertRejectedWithFieldError(submit(body), field);
  }

  @Test
  @DisplayName("AC-001-03 leading and trailing spaces are removed before storing")
  void ac_001_03_textFieldsAreTrimmed() throws Exception {
    Map<String, Object> body = validExternal();
    body.put("firstName", "  Ana ");
    body.put("lastName", " Novak  ");
    body.put("email", "  ana.novak@example.org ");
    body.put("organization", "   Institut Example   ");

    String id = register(body);

    Map<String, Object> row =
        jdbc.queryForMap(
            "SELECT first_name, last_name, email, organization FROM registration WHERE id = ?::uuid",
            id);
    assertThat(row)
        .containsEntry("first_name", "Ana")
        .containsEntry("last_name", "Novak")
        .containsEntry("email", "ana.novak@example.org")
        .containsEntry("organization", "Institut Example");
  }

  @ParameterizedTest(name = "AC-001-04 email \"{0}\" is rejected")
  @ValueSource(
      strings = {
        "ana",
        "ana@",
        "@example.org",
        "ana@example",
        "ana novak@example.org",
        "ana@@example.org",
        "ana@example.org\r\nBcc: victim@example.org"
      })
  void ac_001_04_invalidEmailIsRejected(String email) throws Exception {
    Map<String, Object> body = validExternal();
    body.put("email", email);

    assertRejectedWithFieldError(submit(body), "email");
  }

  static Stream<Arguments> invalidOptionLists() {
    return Stream.of(
        Arguments.of("unknown", List.of("ws-testing", "ws-does-not-exist")),
        Arguments.of("inactive", List.of("ws-retired")),
        Arguments.of("listed twice", List.of("ws-testing", "meal-lunch", "ws-testing")));
  }

  @ParameterizedTest(name = "AC-001-05 {0} option is rejected")
  @MethodSource("invalidOptionLists")
  void ac_001_05_invalidOptionIsRejected(String reason, List<String> optionIds) throws Exception {
    Map<String, Object> body = validExternal();
    body.put("optionIds", new ArrayList<>(optionIds));

    assertRejectedWithFieldError(submit(body), "optionIds");
  }

  @Test
  @DisplayName("AC-001-06 missing mandatory consent is rejected")
  void ac_001_06_missingMandatoryConsentIsRejected() throws Exception {
    Map<String, Object> body = validExternal();
    body.put("consents", new ArrayList<>());

    assertRejectedWithFieldError(submit(body), "consents.privacy");
  }

  @Test
  @DisplayName("AC-001-06 only an optional consent given is rejected")
  void ac_001_06_onlyOptionalConsentIsRejected() throws Exception {
    Map<String, Object> body = validExternal();
    body.put("consents", new ArrayList<>(List.of("photo")));

    assertRejectedWithFieldError(submit(body), "consents.privacy");
  }

  @Test
  @DisplayName("AC-001-07 missing reCAPTCHA token is rejected and nothing is stored")
  void ac_001_07_missingTokenIsRejected() throws Exception {
    Map<String, Object> body = validExternal();
    body.remove("recaptchaToken");

    MvcResult result = submit(body);

    assertThat(result.getResponse().getStatus()).as(body(result)).isEqualTo(400);
    assertNothingChanged();
  }

  @Test
  @DisplayName("AC-001-07 token rejected by the verifier is rejected and nothing is stored")
  void ac_001_07_rejectedTokenIsRejected() throws Exception {
    Map<String, Object> body = validExternal();
    body.put("recaptchaToken", "token-the-verifier-rejects");

    MvcResult result = submit(body);

    assertThat(result.getResponse().getStatus()).as(body(result)).isEqualTo(400);
    assertNothingChanged();
  }
}
