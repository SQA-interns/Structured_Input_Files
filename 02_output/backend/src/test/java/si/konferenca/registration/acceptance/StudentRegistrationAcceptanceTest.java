package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static si.konferenca.registration.acceptance.support.Payloads.external;
import static si.konferenca.registration.acceptance.support.Payloads.student;
import static si.konferenca.registration.acceptance.support.Payloads.with;
import static si.konferenca.registration.acceptance.support.Payloads.without;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.Api;
import si.konferenca.registration.acceptance.support.Payloads;

/** US-002 Student registration. */
class StudentRegistrationAcceptanceTest extends AcceptanceTestBase {

  @Test
  @DisplayName("AC-002-01: a valid student submission is accepted with a registration id")
  void ac_002_01_validStudentSubmissionIsAccepted() {
    Api.Response response = api.register(student());

    assertThat(response.status()).as(response.text()).isEqualTo(201);
    UUID id = UUID.fromString(response.json().read("$.id"));
    Map<String, Object> row = db.registration(id).orElseThrow();
    assertThat(row.get("type")).isEqualTo("STUDENT");
  }

  static Stream<Arguments> missingRequiredFields() {
    return Payloads.STUDENT_REQUIRED.stream()
        .flatMap(
            field ->
                Stream.of(
                    Arguments.of(field, "missing"),
                    Arguments.of(field, ""),
                    Arguments.of(field, "   ")));
  }

  @ParameterizedTest(name = "AC-002-02: {0} = [{1}] is rejected")
  @MethodSource("missingRequiredFields")
  void ac_002_02_missingOrBlankRequiredFieldIsRejected(String field, String value) {
    Map<String, Object> valid = student();
    Map<String, Object> submission =
        "missing".equals(value) ? without(valid, field) : with(valid, field, value);

    Api.Response response = api.register(submission);

    assertRejected(response, field, JSON_COPY_DIR, valid);
  }

  @ParameterizedTest(name = "AC-002-03: email [{0}] is rejected")
  @ValueSource(strings = {"luka.kranjc", "luka@", "@student.example.test", "luka kranjc@x.test"})
  void ac_002_03_invalidEmailIsRejected(String email) {
    Map<String, Object> valid = student();

    Api.Response response = api.register(with(valid, "email", email));

    assertRejected(response, "email", JSON_COPY_DIR, valid);
  }

  @Test
  @DisplayName("AC-002-04: an option not available to students is rejected for a student")
  void ac_002_04_optionNotAvailableToStudentsIsRejected() {
    Map<String, Object> valid = student();
    Map<String, Object> submission = with(valid, "optionIds", List.of("ws-ai", "ws-cloud"));

    Api.Response response = api.register(submission);

    assertRejected(response, "optionIds", JSON_COPY_DIR, valid);
    // Control: the same option is active and accepted for an external participant.
    registerAccepted(with(external(), "optionIds", List.of("ws-cloud")));
  }

  @ParameterizedTest(name = "AC-002-05: consents {0} are rejected")
  @MethodSource("consentLists")
  void ac_002_05_missingMandatoryConsentIsRejected(List<String> consentIds) {
    Map<String, Object> valid = student();

    Api.Response response = api.register(with(valid, "consentIds", consentIds));

    assertRejected(response, "consents.privacy", JSON_COPY_DIR, valid);
  }

  static Stream<List<String>> consentLists() {
    return Stream.of(List.of(), List.of("photo"));
  }

  @ParameterizedTest(name = "AC-002-06: captcha token [{0}] is rejected")
  @ValueSource(strings = {"missing", "", "TEST-FAIL"})
  void ac_002_06_missingOrRejectedCaptchaIsRejected(String token) {
    Map<String, Object> valid = student();
    Map<String, Object> submission =
        "missing".equals(token)
            ? without(valid, "captchaToken")
            : with(valid, "captchaToken", token);

    Api.Response response = api.register(submission);

    assertRejected(response, "captchaToken", JSON_COPY_DIR, valid);
  }
}
