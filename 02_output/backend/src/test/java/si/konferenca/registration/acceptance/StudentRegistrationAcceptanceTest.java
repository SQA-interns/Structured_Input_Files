package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.node.ObjectNode;

/** US-002 student registration, through the REST API. */
class StudentRegistrationAcceptanceTest extends AcceptanceTestBase {

  private static final List<String> REQUIRED =
      List.of("firstName", "lastName", "email", "studyInstitution", "studyProgramme", "studentId");

  static Stream<Arguments> requiredFieldVariants() {
    return REQUIRED.stream()
        .flatMap(
            f -> Stream.of(Arguments.of(f, "missing"), Arguments.of(f, ""), Arguments.of(f, "  ")));
  }

  @Test
  @DisplayName("AC-002-01 valid student submission is accepted with a new registration id")
  void ac00201ValidStudentSubmissionAccepted() {
    String id = assertAccepted(api.postRegistration(Submissions.validStudent()));

    assertThat(AcceptanceEnvironment.query("SELECT type FROM registration WHERE id = ?::uuid", id))
        .singleElement()
        .satisfies(row -> assertThat(row).containsEntry("type", "STUDENT"));
  }

  @ParameterizedTest(name = "AC-002-02 {0} = [{1}] is rejected")
  @MethodSource("requiredFieldVariants")
  @DisplayName("AC-002-02 missing, empty or whitespace required student field is rejected")
  void ac00202RequiredFieldRejected(String field, String value) {
    ObjectNode body = Submissions.validStudent();
    if ("missing".equals(value)) {
      body.remove(field);
    } else {
      body.put(field, value);
    }

    assertRejected(api.postRegistration(body), field);
  }

  @ParameterizedTest(name = "AC-002-03 email [{0}] is rejected")
  @ValueSource(strings = {"luka", "luka@", "@student.example.si", "luka kranjc@x.si", "luka@x"})
  @DisplayName("AC-002-03 invalid email format is rejected")
  void ac00203InvalidEmailRejected(String email) {
    ObjectNode body = Submissions.validStudent();
    body.put("email", email);

    assertRejected(api.postRegistration(body), "email");
  }

  @Test
  @DisplayName("AC-002-04 active option not available to students is rejected")
  void ac00204OptionNotForStudentsRejected() {
    ObjectNode body = Submissions.validStudent();
    body.putArray("optionIds").add("ws-a").add("ws-ext");

    assertRejected(api.postRegistration(body), "options");
  }

  @ParameterizedTest(name = "AC-002-05 privacy consent {0} is rejected")
  @ValueSource(strings = {"false", "absent"})
  @DisplayName("AC-002-05 mandatory consent not given is rejected")
  void ac00205MandatoryConsentMissingRejected(String variant) {
    ObjectNode body = Submissions.validStudent();
    if ("false".equals(variant)) {
      body.putObject("consents").put("privacy", false);
    } else {
      body.putObject("consents");
    }

    assertRejected(api.postRegistration(body), "consents.privacy");
  }

  @ParameterizedTest(name = "AC-002-06 reCAPTCHA token {0} is rejected")
  @ValueSource(strings = {"missing", "rejected-by-verifier"})
  @DisplayName("AC-002-06 missing or rejected reCAPTCHA token is rejected and nothing stored")
  void ac00206CaptchaRejected(String variant) {
    ObjectNode body = Submissions.validStudent();
    if ("missing".equals(variant)) {
      body.remove("recaptchaToken");
    } else {
      body.put("recaptchaToken", "not-a-valid-token");
    }

    assertRejected(api.postRegistration(body), "recaptchaToken");
  }
}
