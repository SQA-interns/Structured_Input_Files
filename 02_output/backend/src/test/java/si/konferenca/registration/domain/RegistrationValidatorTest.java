package si.konferenca.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RegistrationValidatorTest {

  private final RegistrationValidator validator = new RegistrationValidator(Catalogs.sample());

  private static RegistrationForm external(
      String email, List<String> options, List<String> consents) {
    return new RegistrationForm(
        "EXTERNAL", " Ana ", "Novak", email, "IJS", null, null, null, options, consents, "token");
  }

  private static RegistrationForm student(String institution, String programme, String id) {
    return new RegistrationForm(
        "STUDENT",
        "Luka",
        "Kranjc",
        "luka@example.si",
        null,
        institution,
        programme,
        id,
        List.of("ev-night"),
        List.of("data"),
        "token");
  }

  private List<FieldError> errors(RegistrationForm form) {
    try {
      validator.validate(form);
      return List.of();
    } catch (ValidationException e) {
      return e.errors();
    }
  }

  @Test
  void acceptsValidExternalAndTrims() {
    ValidRegistration valid =
        validator.validate(external("ana@example.si", List.of("ws-ai"), List.of("data", "photos")));

    assertThat(valid.type()).isEqualTo(RegistrationType.EXTERNAL);
    assertThat(valid.participant().firstName()).isEqualTo("Ana");
    assertThat(valid.participant().organization()).isEqualTo("IJS");
    assertThat(valid.participant().studentId()).isNull();
    assertThat(valid.options()).extracting(ConferenceOption::id).containsExactly("ws-ai");
    assertThat(valid.consents()).extracting(Consent::id).containsExactly("data", "photos");
  }

  @Test
  void acceptsValidStudent() {
    ValidRegistration valid = validator.validate(student("UM", "RIT", "E1"));

    assertThat(valid.participant().studyInstitution()).isEqualTo("UM");
    assertThat(valid.participant().studyProgramme()).isEqualTo("RIT");
    assertThat(valid.participant().studentId()).isEqualTo("E1");
    assertThat(valid.participant().organization()).isNull();
  }

  @Test
  void studentFieldsAreRequiredAndOrganizationNotAllowed() {
    RegistrationForm form =
        new RegistrationForm(
            "STUDENT",
            "L",
            "K",
            "l@example.si",
            "IJS",
            " ",
            null,
            "",
            List.of(),
            List.of("data"),
            "t");

    assertThat(errors(form))
        .containsExactlyInAnyOrder(
            new FieldError("organization", FieldError.NOT_ALLOWED),
            new FieldError("studyInstitution", FieldError.REQUIRED),
            new FieldError("studyProgramme", FieldError.REQUIRED),
            new FieldError("studentId", FieldError.REQUIRED));
  }

  @Test
  void externalMustNotCarryStudentFields() {
    RegistrationForm form =
        new RegistrationForm(
            "EXTERNAL",
            "A",
            "N",
            "a@example.si",
            "IJS",
            "UM",
            "RIT",
            "E1",
            List.of(),
            List.of("data"),
            "t");

    assertThat(errors(form))
        .containsExactlyInAnyOrder(
            new FieldError("studyInstitution", FieldError.NOT_ALLOWED),
            new FieldError("studyProgramme", FieldError.NOT_ALLOWED),
            new FieldError("studentId", FieldError.NOT_ALLOWED));
  }

  @Test
  void missingOrUnknownTypeIsReported() {
    RegistrationForm missing =
        new RegistrationForm(
            null, "A", "N", "a@example.si", null, null, null, null, null, null, "t");
    RegistrationForm unknown =
        new RegistrationForm(
            "TEACHER", "A", "N", "a@example.si", null, null, null, null, null, null, "t");

    assertThat(errors(missing)).contains(new FieldError("type", FieldError.REQUIRED));
    assertThat(errors(unknown)).contains(new FieldError("type", FieldError.INVALID_VALUE));
  }

  @Test
  void typeOnlyProblemStillRejects() {
    RegistrationForm form =
        new RegistrationForm(
            "", "A", "N", "a@example.si", null, null, null, null, null, List.of("data"), "t");

    assertThatThrownBy(() -> validator.validate(form)).isInstanceOf(ValidationException.class);
  }

  @Test
  void lengthLimitsCountCodePoints() {
    String twoHundredEmoji = "😀".repeat(200);
    RegistrationForm atLimit =
        new RegistrationForm(
            "EXTERNAL",
            twoHundredEmoji,
            "N",
            "a@example.si",
            "IJS",
            null,
            null,
            null,
            List.of(),
            List.of("data"),
            "t");
    RegistrationForm overLimit =
        new RegistrationForm(
            "EXTERNAL",
            "x".repeat(201),
            "N",
            "a@example.si",
            "IJS",
            null,
            null,
            null,
            List.of(),
            List.of("data"),
            "t");

    assertThat(errors(atLimit)).isEmpty();
    assertThat(errors(overLimit)).containsExactly(new FieldError("firstName", FieldError.TOO_LONG));
  }

  @Test
  void controlCharactersAreRejectedButSlovenianLettersAccepted() {
    RegistrationForm control =
        new RegistrationForm(
            "EXTERNAL",
            "A\u0085B",
            "Žagar",
            "a@example.si",
            "Čšž",
            null,
            null,
            null,
            List.of(),
            List.of("data"),
            "t");

    assertThat(errors(control))
        .containsExactly(new FieldError("firstName", FieldError.INVALID_CHARACTERS));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "ana@example.si",
        "ana.novak+konf@sub.example.co",
        "a_b-c@x-y.si",
        "o'brien@example.com"
      })
  void validEmails(String email) {
    assertThat(RegistrationValidator.validEmail(email)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "ana",
        "@example.si",
        "ana@",
        "ana@@example.si",
        "ana@example",
        "ana@-example.si",
        "ana@example-.si",
        "ana..novak@example.si",
        "ana.@example.si",
        "ana@example.s1",
        "ana@exa mple.si"
      })
  void invalidEmails(String email) {
    assertThat(RegistrationValidator.validEmail(email)).isFalse();
  }

  @Test
  void localPartLongerThan64IsInvalid() {
    assertThat(RegistrationValidator.validEmail("a".repeat(64) + "@example.si")).isTrue();
    assertThat(RegistrationValidator.validEmail("a".repeat(65) + "@example.si")).isFalse();
  }

  @Test
  void optionProblemsAreReportedOncePerCode() {
    RegistrationForm form =
        external(
            "a@example.si",
            Arrays.asList("x", "y", null, "ws-old", "ev-night", "ws-ai", "ws-ai"),
            List.of("data"));

    assertThat(errors(form))
        .containsExactly(
            new FieldError("optionIds", FieldError.UNKNOWN_OPTION),
            new FieldError("optionIds", FieldError.OPTION_NOT_AVAILABLE),
            new FieldError("optionIds", FieldError.DUPLICATE_OPTION));
  }

  @Test
  void optionWithoutAudienceIsOpenToBothTypes() {
    assertThat(errors(external("a@example.si", List.of("meal-lunch"), List.of("data")))).isEmpty();
  }

  @Test
  void consentsMandatoryAndKnown() {
    RegistrationForm form =
        external("a@example.si", List.of(), Arrays.asList("photos", "nope", null));

    assertThat(errors(form))
        .containsExactlyInAnyOrder(
            new FieldError("consentIds", FieldError.UNKNOWN_CONSENT),
            new FieldError("consentIds", FieldError.UNKNOWN_CONSENT),
            new FieldError("consents.data", FieldError.CONSENT_REQUIRED));
  }

  @Test
  void captchaTokenRequired() {
    RegistrationForm form =
        new RegistrationForm(
            "EXTERNAL",
            "A",
            "N",
            "a@example.si",
            "IJS",
            null,
            null,
            null,
            List.of(),
            List.of("data"),
            " ");

    assertThat(errors(form)).containsExactly(new FieldError("captchaToken", FieldError.REQUIRED));
  }

  @Test
  void emailTooLongBeforeFormat() {
    String email = "a".repeat(64) + "@" + "b".repeat(190) + ".si";

    assertThat(errors(external(email, List.of(), List.of("data"))))
        .containsExactly(new FieldError("email", FieldError.TOO_LONG));
  }

  @Test
  void invalidEmailReported() {
    assertThat(errors(external("ana@example", List.of(), List.of("data"))))
        .containsExactly(new FieldError("email", FieldError.INVALID_EMAIL));
  }
}
