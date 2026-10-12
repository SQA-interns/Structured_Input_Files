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

/** US-002 Student registration, through POST /api/registrations. */
class StudentRegistrationAcceptanceTest extends AcceptanceTestBase {

  @DynamicPropertySource
  static void settings(DynamicPropertyRegistry registry) {
    TestStack.register(registry);
  }

  @Test
  @DisplayName("AC-002-01 valid student submission is accepted with 201 and the new id")
  void ac_002_01_validStudentSubmissionIsAccepted() throws Exception {
    MvcResult result = submit(validStudent());

    assertThat(result.getResponse().getStatus()).as(body(result)).isEqualTo(201);
    String id = JsonPath.read(body(result), "$.id");
    assertThat(id).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM registration WHERE id = ?::uuid AND type = 'STUDENT'",
                Integer.class,
                id))
        .isEqualTo(1);
  }

  static Stream<Arguments> requiredFieldVariants() {
    List<String> fields =
        List.of(
            "firstName", "lastName", "email", "studyInstitution", "studyProgramme", "studentId");
    List<String> variants = List.of("missing", "empty", "whitespace");
    return fields.stream().flatMap(f -> variants.stream().map(v -> Arguments.of(f, v)));
  }

  @ParameterizedTest(name = "AC-002-02 {0} {1} is rejected")
  @MethodSource("requiredFieldVariants")
  void ac_002_02_missingRequiredFieldIsRejected(String field, String variant) throws Exception {
    Map<String, Object> body = validStudent();
    switch (variant) {
      case "missing" -> body.remove(field);
      case "empty" -> body.put(field, "");
      default -> body.put(field, "   ");
    }

    assertRejectedWithFieldError(submit(body), field);
  }

  @ParameterizedTest(name = "AC-002-03 email \"{0}\" is rejected")
  @ValueSource(strings = {"luka", "luka@", "@student.example.org", "luka kranjc@example.org"})
  void ac_002_03_invalidEmailIsRejected(String email) throws Exception {
    Map<String, Object> body = validStudent();
    body.put("email", email);

    assertRejectedWithFieldError(submit(body), "email");
  }

  @ParameterizedTest(name = "AC-002-04 option {0} not available to students is rejected")
  @ValueSource(strings = {"ws-security", "meal-dinner"})
  void ac_002_04_optionNotAvailableToStudentsIsRejected(String optionId) throws Exception {
    Map<String, Object> body = validStudent();
    body.put("optionIds", new ArrayList<>(List.of("ws-testing", optionId)));

    assertRejectedWithFieldError(submit(body), "optionIds");
  }

  @Test
  @DisplayName("AC-002-04 the same option is accepted for an external participant")
  void ac_002_04_sameOptionIsAcceptedForExternal() throws Exception {
    Map<String, Object> body = validExternal();
    body.put("optionIds", new ArrayList<>(List.of("ws-security", "meal-dinner")));

    register(body);
  }

  @Test
  @DisplayName("AC-002-05 missing mandatory consent is rejected")
  void ac_002_05_missingMandatoryConsentIsRejected() throws Exception {
    Map<String, Object> body = validStudent();
    body.remove("consents");

    assertRejectedWithFieldError(submit(body), "consents.privacy");
  }

  @Test
  @DisplayName("AC-002-06 missing reCAPTCHA token is rejected and nothing is stored")
  void ac_002_06_missingTokenIsRejected() throws Exception {
    Map<String, Object> body = validStudent();
    body.put("recaptchaToken", "");

    MvcResult result = submit(body);

    assertThat(result.getResponse().getStatus()).as(body(result)).isEqualTo(400);
    assertNothingChanged();
  }

  @Test
  @DisplayName("AC-002-06 token rejected by the verifier is rejected and nothing is stored")
  void ac_002_06_rejectedTokenIsRejected() throws Exception {
    Map<String, Object> body = validStudent();
    body.put("recaptchaToken", "token-the-verifier-rejects");

    MvcResult result = submit(body);

    assertThat(result.getResponse().getStatus()).as(body(result)).isEqualTo(400);
    assertNothingChanged();
  }
}
