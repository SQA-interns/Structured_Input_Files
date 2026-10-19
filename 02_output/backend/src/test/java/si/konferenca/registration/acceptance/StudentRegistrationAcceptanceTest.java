package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MvcResult;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.Submissions;

/** US-002: student registration through POST /api/registrations. */
class StudentRegistrationAcceptanceTest extends AcceptanceTestBase {

  @Test
  void AC_002_01_validStudentSubmissionIsAcceptedWithNewId() throws Exception {
    MvcResult result = submit(Submissions.validStudent());

    assertThat(result.getResponse().getStatus()).isEqualTo(201);
    String id = json(result).path("id").asString();
    assertThat(UUID.fromString(id)).isNotNull();
    assertThat(registrationCount()).isEqualTo(1);
  }

  static Stream<Arguments> requiredFieldVariants() {
    return Stream.of(
            "firstName", "lastName", "email", "studyInstitution", "studyProgramme", "studentId")
        .flatMap(
            field ->
                Stream.of(
                    Arguments.of(field, "missing"),
                    Arguments.of(field, ""),
                    Arguments.of(field, "  \t  ")));
  }

  @ParameterizedTest(name = "AC-002-02 {0} = [{1}]")
  @MethodSource("requiredFieldVariants")
  void AC_002_02_missingEmptyOrBlankRequiredFieldIsRejected(String field, String value)
      throws Exception {
    Map<String, Object> body = Submissions.validStudent();
    String email = (String) body.get("email");
    if ("missing".equals(value)) {
      body.remove(field);
    } else {
      body.put(field, value);
    }

    MvcResult result = submit(body);

    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    assertThat(errorFields(result)).contains(field);
    assertNothingStoredOrSent(email);
  }

  @ParameterizedTest(name = "AC-002-03 email = [{0}]")
  @ValueSource(strings = {"luka.kranjc", "luka@", "@student.uni-lj.si", "luka kranjc@uni-lj.si"})
  void AC_002_03_invalidEmailFormatIsRejected(String invalidEmail) throws Exception {
    Map<String, Object> body = Submissions.validStudent();
    body.put("email", invalidEmail);

    MvcResult result = submit(body);

    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    assertThat(errorFields(result)).contains("email");
    assertThat(registrationCount()).isZero();
    assertThat(jsonCopies()).isEmpty();
    assertThat(mailpit.messagesToAfterGracePeriod(invalidEmail)).isEmpty();
  }

  @Test
  void AC_002_04_optionNotAvailableToStudentsIsRejected() throws Exception {
    Map<String, Object> body = Submissions.validStudent();
    String email = (String) body.get("email");
    body.put("optionIds", List.of("ws-ai", "oth-tour"));

    MvcResult result = submit(body);

    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    assertThat(errorFields(result)).contains("optionIds");
    assertNothingStoredOrSent(email);
  }

  @Test
  void AC_002_05_missingMandatoryConsentIsRejected() throws Exception {
    Map<String, Object> body = Submissions.validStudent();
    String email = (String) body.get("email");
    body.put("consentIds", List.of("photos"));

    MvcResult result = submit(body);

    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    assertThat(errorFields(result)).contains("consents.privacy");
    assertNothingStoredOrSent(email);
  }

  @ParameterizedTest(name = "AC-002-06 token = [{0}]")
  @ValueSource(strings = {"missing", "", "test-fail"})
  void AC_002_06_missingOrRejectedRecaptchaTokenIsRejected(String token) throws Exception {
    Map<String, Object> body = Submissions.validStudent();
    String email = (String) body.get("email");
    if ("missing".equals(token)) {
      body.remove("recaptchaToken");
    } else {
      body.put("recaptchaToken", token);
    }

    MvcResult result = submit(body);

    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    assertThat(errorFields(result)).contains("recaptchaToken");
    assertNothingStoredOrSent(email);
  }
}
