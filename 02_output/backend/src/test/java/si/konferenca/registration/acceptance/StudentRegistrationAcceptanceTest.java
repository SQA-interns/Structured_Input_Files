package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import tools.jackson.databind.JsonNode;

/** US-002 Student registration, through the REST API. */
class StudentRegistrationAcceptanceTest extends AcceptanceTestBase {

  @Test
  void ac_002_01_formConfigOffersStudentOptions() {
    HttpResponse<String> response = get("/api/form-config");

    assertThat(response.statusCode()).isEqualTo(200);
    List<String> studentOptions =
        json(response)
            .path("options")
            .valueStream()
            .filter(
                o -> o.path("audience").valueStream().anyMatch(a -> "STUDENT".equals(a.asString())))
            .map(o -> o.path("id").asString())
            .toList();
    assertThat(studentOptions)
        .containsExactlyInAnyOrder("ws-ai", "ev-student-night", "meal-lunch", "other-tour");
  }

  @Test
  void ac_002_02_validStudentRegistrationIsAcceptedAndExported() {
    String email = uniqueEmail("ac-002-02");

    JsonNode accepted = registerAccepted(validStudent(email));

    Map<String, String> row = exportRow(exportRows(), accepted.path("reference").asString());
    assertThat(row)
        .containsEntry("Type", "STUDENT")
        .containsEntry("First name", "Luka")
        .containsEntry("Last name", "Kranjc")
        .containsEntry("Email", email)
        .containsEntry("Study institution", "Univerza v Mariboru")
        .containsEntry("Study programme", "Računalništvo in informatika")
        .containsEntry("Student ID", "E1234567")
        .containsEntry("Events", "Študentski večer")
        .containsEntry("Meals", "Kosilo");
    assertThat(row.get("Consents")).contains("data-processing");
  }

  static Stream<Arguments> missingStudentFields() {
    return Stream.of("studyInstitution", "studyProgramme", "studentId")
        .flatMap(
            field ->
                Stream.of(
                    Arguments.of(field, null), Arguments.of(field, ""), Arguments.of(field, "  ")));
  }

  @ParameterizedTest(name = "AC-002-03 {0} = [{1}]")
  @MethodSource("missingStudentFields")
  void ac_002_03_missingStudentFieldIsRejected(String field, String value) {
    String email = uniqueEmail("ac-002-03");
    Map<String, Object> body = validStudent(email);
    if (value == null) {
      body.remove(field);
    } else {
      body.put(field, value);
    }
    long before = registrationCount();

    HttpResponse<String> response = register(body);

    assertFieldError(response, 400, field, "REQUIRED");
    assertThat(registrationCount()).isEqualTo(before);
  }

  @Test
  void ac_002_04_optionNotForStudentsIsNotOfferedToStudents() {
    JsonNode gala =
        json(get("/api/form-config"))
            .path("options")
            .valueStream()
            .filter(o -> "ev-gala".equals(o.path("id").asString()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("option ev-gala is not offered"));

    assertThat(gala.path("audience").valueStream().map(JsonNode::asString))
        .containsExactly("EXTERNAL");
  }

  @Test
  void ac_002_04_studentSelectingAnExternalOnlyOptionIsRejected() {
    String email = uniqueEmail("ac-002-04");
    Map<String, Object> body = validStudent(email);
    body.put("optionIds", List.of("meal-lunch", "ev-gala"));

    HttpResponse<String> response = register(body);

    assertFieldError(response, 400, "optionIds", "OPTION_NOT_AVAILABLE");
    assertThat(registrationCountByEmail(email)).isZero();
  }

  @Test
  void ac_002_04_externalSelectingAStudentOnlyOptionIsRejected() {
    String email = uniqueEmail("ac-002-04-ext");
    Map<String, Object> body = validExternal(email);
    body.put("optionIds", List.of("ev-student-night"));

    HttpResponse<String> response = register(body);

    assertFieldError(response, 400, "optionIds", "OPTION_NOT_AVAILABLE");
    assertThat(registrationCountByEmail(email)).isZero();
  }
}
