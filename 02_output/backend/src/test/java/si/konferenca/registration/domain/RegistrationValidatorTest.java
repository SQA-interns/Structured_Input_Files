package si.konferenca.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RegistrationValidatorTest {

  static final ConferenceCatalog CATALOG =
      new ConferenceCatalog() {
        @Override
        public List<ConferenceOption> options() {
          return List.of(
              new ConferenceOption(
                  "ws",
                  "Workshop",
                  OptionCategory.WORKSHOP,
                  true,
                  EnumSet.allOf(RegistrationType.class)),
              new ConferenceOption(
                  "ext",
                  "External only",
                  OptionCategory.EVENT,
                  true,
                  Set.of(RegistrationType.EXTERNAL)),
              new ConferenceOption(
                  "old",
                  "Inactive",
                  OptionCategory.MEAL,
                  false,
                  EnumSet.allOf(RegistrationType.class)));
        }

        @Override
        public List<ConsentDefinition> consents() {
          return List.of(
              new ConsentDefinition("privacy", "Privacy text", true),
              new ConsentDefinition("photo", "Photo text", false));
        }
      };

  private final RegistrationValidator validator = new RegistrationValidator(CATALOG);

  static RegistrationSubmission external(List<String> options, List<String> consents) {
    return new RegistrationSubmission(
        "EXTERNAL",
        " Ana ",
        "Novak",
        "ana@example.si",
        "IJS",
        null,
        null,
        null,
        options,
        consents,
        "token",
        "127.0.0.1");
  }

  static RegistrationSubmission student() {
    return new RegistrationSubmission(
        "STUDENT",
        "Luka",
        "Kranjc",
        "luka@example.si",
        "",
        "UL",
        "RI",
        "63200001",
        List.of("ws"),
        List.of("privacy"),
        "token",
        "127.0.0.1");
  }

  private List<FieldError> errorsOf(RegistrationSubmission s) {
    try {
      validator.validate(s);
    } catch (RegistrationRejectedException e) {
      return e.errors();
    }
    throw new AssertionError("expected a rejection");
  }

  @Test
  void acceptsExternalAndTrimsValues() {
    ValidSubmission v =
        validator.validate(external(List.of("ws", "ext"), List.of("privacy", "photo")));

    assertThat(v.type()).isEqualTo(RegistrationType.EXTERNAL);
    assertThat(v.participant().firstName()).isEqualTo("Ana");
    assertThat(v.participant().organization()).isEqualTo("IJS");
    assertThat(v.participant().studentId()).isNull();
    assertThat(v.options()).extracting(ConferenceOption::id).containsExactly("ws", "ext");
    assertThat(v.consents()).extracting(ConsentDefinition::id).containsExactly("privacy", "photo");
    assertThat(v.captchaToken()).isEqualTo("token");
  }

  @Test
  void acceptsStudentWithEmptyOrganization() {
    ValidSubmission v = validator.validate(student());

    assertThat(v.participant().studyInstitution()).isEqualTo("UL");
    assertThat(v.participant().studyProgramme()).isEqualTo("RI");
    assertThat(v.participant().studentId()).isEqualTo("63200001");
    assertThat(v.participant().organization()).isNull();
  }

  @Test
  void rejectsFieldsOfTheOtherType() {
    RegistrationSubmission s =
        new RegistrationSubmission(
            "STUDENT",
            "Luka",
            "Kranjc",
            "luka@example.si",
            "ACME",
            "UL",
            "RI",
            "1",
            List.of(),
            List.of("privacy"),
            "t",
            null);
    assertThat(errorsOf(s))
        .containsExactly(new FieldError("organization", RegistrationValidator.NOT_ALLOWED));

    RegistrationSubmission e =
        new RegistrationSubmission(
            "EXTERNAL",
            "Ana",
            "Novak",
            "ana@example.si",
            "IJS",
            "UL",
            "RI",
            "1",
            List.of(),
            List.of("privacy"),
            "t",
            null);
    assertThat(errorsOf(e))
        .extracting(FieldError::field)
        .containsExactly("studyInstitution", "studyProgramme", "studentId");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "external", "VISITOR"})
  void rejectsUnknownTypeAndSkipsTypeFields(String type) {
    RegistrationSubmission s =
        new RegistrationSubmission(
            type,
            "Ana",
            "Novak",
            "ana@example.si",
            null,
            null,
            null,
            null,
            List.of("ws"),
            List.of("privacy"),
            "t",
            null);

    assertThat(errorsOf(s))
        .containsExactly(new FieldError("type", RegistrationValidator.INVALID_TYPE));
  }

  @Test
  void reportsAllMissingFieldsTogether() {
    RegistrationSubmission s =
        new RegistrationSubmission(
            "STUDENT", null, " ", null, null, null, "", null, null, null, null, null);

    assertThat(errorsOf(s))
        .extracting(FieldError::field)
        .containsExactly(
            "firstName",
            "lastName",
            "email",
            "studyInstitution",
            "studyProgramme",
            "studentId",
            "consents.privacy",
            "captchaToken");
  }

  @Test
  void enforcesMaximumLengths() {
    RegistrationSubmission s =
        new RegistrationSubmission(
            "STUDENT",
            "a".repeat(101),
            "b".repeat(100),
            "x".repeat(245) + "@example.si",
            null,
            "c".repeat(201),
            "d".repeat(200),
            "e".repeat(51),
            List.of(),
            List.of("privacy"),
            "t",
            null);

    assertThat(errorsOf(s))
        .containsExactly(
            new FieldError("firstName", "Use at most 100 characters."),
            new FieldError("email", RegistrationValidator.INVALID_EMAIL),
            new FieldError("studyInstitution", "Use at most 200 characters."),
            new FieldError("studentId", "Use at most 50 characters."));
  }

  @Test
  void acceptsValuesAtTheMaximumLength() {
    RegistrationSubmission s =
        new RegistrationSubmission(
            "STUDENT",
            "a".repeat(100),
            "b",
            "x".repeat(243) + "@example.si",
            null,
            "c".repeat(200),
            "d",
            "e".repeat(50),
            List.of(),
            List.of("privacy"),
            "t",
            null);

    assertThat(validator.validate(s).participant().email()).hasSize(254);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "a@b",
        "a b@c.si",
        "a@b c.si",
        "a<b>@c.si",
        "a@b.si\nBcc: x@y.si",
        "a,b@c.si",
        "\"a\"@c.si"
      })
  void rejectsInvalidEmails(String email) {
    RegistrationSubmission s =
        new RegistrationSubmission(
            "EXTERNAL",
            "Ana",
            "Novak",
            email,
            "IJS",
            null,
            null,
            null,
            List.of(),
            List.of("privacy"),
            "t",
            null);

    assertThat(errorsOf(s))
        .containsExactly(new FieldError("email", RegistrationValidator.INVALID_EMAIL));
  }

  @Test
  void rejectsDuplicateOptions() {
    assertThat(errorsOf(external(List.of("ws", "ws"), List.of("privacy"))))
        .containsExactly(new FieldError("optionIds", RegistrationValidator.DUPLICATE_OPTION));
  }

  @Test
  void rejectsUnknownInactiveNullAndUnavailableOptionsOnce() {
    assertThat(errorsOf(external(List.of("nope", "old"), List.of("privacy"))))
        .containsExactly(new FieldError("optionIds", RegistrationValidator.UNAVAILABLE_OPTION));
    assertThat(errorsOf(external(List.of("old"), List.of("privacy"))))
        .containsExactly(new FieldError("optionIds", RegistrationValidator.UNAVAILABLE_OPTION));
    assertThat(
            errorsOf(external(new ArrayList<>(Arrays.asList((String) null)), List.of("privacy"))))
        .containsExactly(new FieldError("optionIds", RegistrationValidator.UNAVAILABLE_OPTION));
    RegistrationSubmission s =
        new RegistrationSubmission(
            "STUDENT",
            "Luka",
            "Kranjc",
            "luka@example.si",
            null,
            "UL",
            "RI",
            "1",
            List.of("ext"),
            List.of("privacy"),
            "t",
            null);
    assertThat(errorsOf(s))
        .containsExactly(new FieldError("optionIds", RegistrationValidator.UNAVAILABLE_OPTION));
  }

  @Test
  void treatsMissingOptionListAsNoOptions() {
    assertThat(validator.validate(external(null, List.of("privacy"))).options()).isEmpty();
  }

  @Test
  void rejectsMissingMandatoryAndUnknownConsents() {
    assertThat(errorsOf(external(List.of(), List.of("photo", "spam"))))
        .containsExactly(
            new FieldError("consents.privacy", RegistrationValidator.CONSENT_REQUIRED),
            new FieldError("consentIds", RegistrationValidator.UNKNOWN_CONSENT));
  }

  @Test
  void optionalConsentMayBeOmitted() {
    assertThat(validator.validate(external(List.of(), List.of("privacy"))).consents())
        .extracting(ConsentDefinition::id)
        .containsExactly("privacy");
  }

  @Test
  void rejectsBlankCaptchaToken() {
    RegistrationSubmission s =
        new RegistrationSubmission(
            "EXTERNAL",
            "Ana",
            "Novak",
            "ana@example.si",
            "IJS",
            null,
            null,
            null,
            List.of(),
            List.of("privacy"),
            "  ",
            null);

    assertThatThrownBy(() -> validator.validate(s))
        .isInstanceOfSatisfying(
            RegistrationRejectedException.class,
            e ->
                assertThat(e.errors())
                    .containsExactly(
                        new FieldError("captchaToken", RegistrationValidator.REQUIRED)));
  }
}
