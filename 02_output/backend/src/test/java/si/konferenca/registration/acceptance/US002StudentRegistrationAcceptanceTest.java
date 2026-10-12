package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** US-002 student registration, through {@code POST /api/registrations}. */
class US002StudentRegistrationAcceptanceTest extends AcceptanceTestBase {

  @Test
  @DisplayName("AC-002-03 a valid student registration is accepted and returns its id")
  void ac002_03_validStudentRegistrationIsAccepted() throws Exception {
    Response response = api.postRegistration(Registrations.validStudent());

    assertThat(response.status()).as(response.toString()).isEqualTo(201);
    String id = response.json("$.id");
    assertThat((String) response.json("$.type")).isEqualTo("STUDENT");
    assertThat((List<String>) response.json("$.selectedOptions[*].id"))
        .containsExactly("ws-b", "other-fair");
    Map<String, Object> row = db.registration(id);
    assertThat(row).isNotNull();
    assertThat(row.get("study_institution")).isEqualTo("University of Ljubljana");
    assertThat(row.get("study_programme")).isEqualTo("Computer Science");
    assertThat(row.get("student_id")).isEqualTo("63210001");
  }

  @ParameterizedTest(name = "AC-002-04 {0} empty or blank is rejected")
  @ValueSource(strings = {"studyInstitution", "studyProgramme", "studentId"})
  void ac002_04_studentFieldEmptyOrBlankIsRejected(String field) throws Exception {
    for (Object value : new Object[] {"", "  ", null}) {
      Map<String, Object> request = Registrations.validStudent();
      request.put(field, value);

      Response response = api.postRegistration(request);

      assertThat(response.status()).as("%s=%s: %s", field, value, response).isEqualTo(400);
      assertThat(response.errorFields()).contains(field);
    }
    assertNothingStoredCopiedOrSent();
  }

  @Test
  @DisplayName("AC-002-05 a student registration with an invalid email is rejected")
  void ac002_05_invalidEmailIsRejected() throws Exception {
    Map<String, Object> request = Registrations.validStudent();
    request.put("email", "luka.kranjc.example.si");

    Response response = api.postRegistration(request);

    assertThat(response.status()).as(response.toString()).isEqualTo(400);
    assertThat(response.errorFields()).contains("email");
    assertNothingStoredCopiedOrSent();
  }

  @Test
  @DisplayName("AC-002-06 an active option not open to students is rejected")
  void ac002_06_optionNotOpenToStudentsIsRejected() throws Exception {
    Map<String, Object> request = Registrations.validStudent();
    request.put("optionIds", List.of("ws-a", "ws-ext"));

    Response response = api.postRegistration(request);

    assertThat(response.status()).as(response.toString()).isEqualTo(400);
    assertThat(response.errorFields()).contains("optionIds");
    assertNothingStoredCopiedOrSent();
  }

  @Test
  @DisplayName("AC-002-07 a student registration without the mandatory consent is rejected")
  void ac002_07_missingConsentIsRejected() throws Exception {
    Map<String, Object> request = Registrations.validStudent();
    request.put("consentGiven", false);

    Response response = api.postRegistration(request);

    assertThat(response.status()).as(response.toString()).isEqualTo(400);
    assertThat(response.errorFields()).contains("consentGiven");
    assertNothingStoredCopiedOrSent();
  }

  @Test
  @DisplayName("AC-002-08 a student registration with a rejected token is rejected")
  void ac002_08_rejectedTokenIsRejected() throws Exception {
    Map<String, Object> request = Registrations.validStudent();
    request.put("recaptchaToken", "forged-token");

    Response response = api.postRegistration(request);

    assertThat(response.status()).as(response.toString()).isEqualTo(400);
    assertThat(response.errorFields()).contains("recaptchaToken");
    assertNothingStoredCopiedOrSent();
  }
}
