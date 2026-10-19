package si.konferenca.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class StudentRegistrationValidatorTest {

  final RegistrationValidator validator =
      new RegistrationValidator(RegistrationValidatorTest.CONFIG);

  static RegistrationSubmission student(
      String institution, String programme, String studentId, List<String> optionIds) {
    return new RegistrationSubmission(
        "STUDENT",
        "Luka",
        "Kranjc",
        "luka@example.com",
        "Ignored organization",
        institution,
        programme,
        studentId,
        optionIds,
        List.of("privacy"),
        "token");
  }

  static List<String> fields(ValidationResult result) {
    return result.errors().stream().map(FieldError::field).toList();
  }

  @Test
  void AC_002_01_validStudentSubmissionKeepsTrimmedStudentFieldsOnly() {
    ValidationResult result =
        validator.validate(student(" Univerza ", "Računalništvo ", " 6321", List.of("oth-m")));

    assertThat(result.isValid()).isTrue();
    ValidatedRegistration r = result.registration();
    assertThat(r.type()).isEqualTo(RegistrationType.STUDENT);
    assertThat(r.studyInstitution()).isEqualTo("Univerza");
    assertThat(r.studyProgramme()).isEqualTo("Računalništvo");
    assertThat(r.studentId()).isEqualTo("6321");
    assertThat(r.organization()).isNull();
    assertThat(r.options()).extracting(ConferenceOption::id).containsExactly("oth-m");
  }

  @ParameterizedTest(name = "AC-002-02 [{0}]")
  @NullSource
  @ValueSource(strings = {"", "  \t"})
  void AC_002_02_eachRequiredStudentFieldMustNotBeBlank(String blank) {
    assertThat(fields(validator.validate(student(blank, "P", "1", List.of()))))
        .containsExactly("studyInstitution");
    assertThat(fields(validator.validate(student("U", blank, "1", List.of()))))
        .containsExactly("studyProgramme");
    assertThat(fields(validator.validate(student("U", "P", blank, List.of()))))
        .containsExactly("studentId");
  }

  @Test
  void AC_002_02_tooLongStudentFieldsAreRejected() {
    String tooLong = "x".repeat(201);

    assertThat(fields(validator.validate(student(tooLong, tooLong, tooLong, List.of()))))
        .containsExactlyInAnyOrder("studyInstitution", "studyProgramme", "studentId");
  }

  @Test
  void AC_002_04_optionNotAvailableToStudentsIsRejected() {
    assertThat(fields(validator.validate(student("U", "P", "1", List.of("ws-a", "oth-t")))))
        .containsExactly("optionIds");
  }

  @Test
  void AC_002_04_optionNotAvailableToExternalsIsRejectedToo() {
    RegistrationSubmission external =
        new RegistrationSubmission(
            "EXTERNAL",
            "Ana",
            "Novak",
            "ana@example.com",
            "IJS",
            null,
            null,
            null,
            List.of("oth-m"),
            List.of("privacy"),
            "token");

    assertThat(fields(validator.validate(external))).containsExactly("optionIds");
  }
}
