package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import si.konferenca.registration.acceptance.support.DefaultContextAcceptanceTest;
import si.konferenca.registration.acceptance.support.Payloads;

/** US-002 Student registration (AC-002-01 … AC-002-06). */
class StudentRegistrationAcceptanceTest extends DefaultContextAcceptanceTest {

  @Test
  void AC_002_01_validStudentSubmissionIsAcceptedWithNewId() {
    String email = Payloads.uniqueEmail("ac00201");

    HttpResponse<String> response = postRegistration(Payloads.student(email));

    assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
    String id = json(response.body()).path("id").asString();
    assertThat(UUID.fromString(id)).isNotNull();
    assertThat(
            DB.queryForObject(
                "SELECT registration_type FROM registration WHERE id = ?::uuid", String.class, id))
        .isEqualTo("STUDENT");
  }

  static Stream<Arguments> requiredFieldsWithBadValues() {
    return Stream.of(
            "firstName", "lastName", "email", "studyInstitution", "studyProgramme", "studentId")
        .flatMap(
            field ->
                Stream.of(
                    Arguments.of(field, "missing"),
                    Arguments.of(field, ""),
                    Arguments.of(field, " \t ")));
  }

  @ParameterizedTest(name = "{0} = [{1}]")
  @MethodSource("requiredFieldsWithBadValues")
  void AC_002_02_missingEmptyOrBlankRequiredFieldIsRejected(String field, String value) {
    String email = Payloads.uniqueEmail("ac00202");
    Map<String, Object> body = Payloads.student(email);
    if ("missing".equals(value)) {
      body.remove(field);
    } else {
      body.put(field, value);
    }

    HttpResponse<String> response = postRegistration(body);

    assertRejectedWithFieldError(response, field);
    assertNothingStoredOrSent(email);
  }

  @ParameterizedTest
  @ValueSource(strings = {"student", "bor@", "@uni.example", "bor@uni", "bor kranjc@uni.example"})
  void AC_002_03_invalidEmailFormatIsRejected(String invalidEmail) {
    String marker = "S" + UUID.randomUUID().toString().substring(0, 12);
    Map<String, Object> body = Payloads.student(invalidEmail);
    body.put("studentId", marker);

    HttpResponse<String> response = postRegistration(body);

    assertRejectedWithFieldError(response, "email");
    assertThat(
            DB.queryForObject(
                "SELECT count(*) FROM registration WHERE student_id = ?", Integer.class, marker))
        .isZero();
    assertThat(copiesMentioning(marker)).isEmpty();
    assertThat(MAIL.countMentioning(marker)).isZero();
  }

  @Test
  void AC_002_04_optionNotAvailableToStudentsIsRejected() {
    String email = Payloads.uniqueEmail("ac00204");
    Map<String, Object> body = Payloads.student(email);
    body.put("optionIds", List.of("ws-testing", "ws-security"));

    HttpResponse<String> response = postRegistration(body);

    assertRejectedWithFieldError(response, "optionIds");
    assertNothingStoredOrSent(email);
  }

  @Test
  void AC_002_05_missingMandatoryConsentIsRejected() {
    String email = Payloads.uniqueEmail("ac00205");
    Map<String, Object> body = Payloads.student(email);
    body.put("consents", List.of());

    HttpResponse<String> response = postRegistration(body);

    assertRejectedWithFieldError(response, "consents.privacy");
    assertNothingStoredOrSent(email);
  }

  @Test
  void AC_002_06_missingRecaptchaTokenIsRejectedAndNothingStored() {
    String email = Payloads.uniqueEmail("ac00206");
    Map<String, Object> body = Payloads.student(email);
    body.put("recaptchaToken", "");

    HttpResponse<String> response = postRegistration(body);

    assertRejectedWithFieldError(response, "recaptchaToken");
    assertNothingStoredOrSent(email);
  }

  @Test
  void AC_002_06_rejectedRecaptchaTokenIsRejectedAndNothingStored() {
    String email = Payloads.uniqueEmail("ac00206b");
    Map<String, Object> body = Payloads.student(email);
    body.put("recaptchaToken", "token-the-verifier-rejects");

    HttpResponse<String> response = postRegistration(body);

    assertRejectedWithFieldError(response, "recaptchaToken");
    assertNothingStoredOrSent(email);
  }
}
