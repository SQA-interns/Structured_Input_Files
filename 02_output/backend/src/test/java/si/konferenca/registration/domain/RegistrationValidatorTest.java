package si.konferenca.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Covers AC-001-01, AC-001-02, AC-001-03, AC-001-04, AC-001-05, AC-001-06, AC-001-07; the consent
 * and token rules do not depend on the registration type, so they are also the unit tests of
 * AC-002-05 and AC-002-06.
 */
class RegistrationValidatorTest {

  static final ConferenceConfiguration CONFIG =
      ConferenceConfiguration.of(
          "Conf",
          List.of(
              new ConsentDefinition("privacy", "I agree.", true),
              new ConsentDefinition("photos", "Photos.", false)),
          List.of(
              new ConferenceOption("ws-a", "Workshop A", OptionCategory.WORKSHOP, true, null),
              new ConferenceOption("ws-old", "Old", OptionCategory.WORKSHOP, false, null),
              new ConferenceOption("ev-d", "Dinner", OptionCategory.EVENT, true, null),
              new ConferenceOption(
                  "oth-t", "Tour", OptionCategory.OTHER, true, Set.of(RegistrationType.EXTERNAL)),
              new ConferenceOption(
                  "oth-m",
                  "Mentoring",
                  OptionCategory.OTHER,
                  true,
                  Set.of(RegistrationType.STUDENT))));

  final RegistrationValidator validator = new RegistrationValidator(CONFIG);

  static RegistrationSubmission external(
      String firstName, String lastName, String email, String organization) {
    return new RegistrationSubmission(
        "EXTERNAL",
        firstName,
        lastName,
        email,
        organization,
        null,
        null,
        null,
        List.of("ev-d", "ws-a"),
        List.of("privacy"),
        "token");
  }

  static RegistrationSubmission validExternal() {
    return external("Ana", "Novak", "ana@example.com", "IJS");
  }

  static RegistrationSubmission withOptions(RegistrationSubmission s, List<String> optionIds) {
    return new RegistrationSubmission(
        s.type(),
        s.firstName(),
        s.lastName(),
        s.email(),
        s.organization(),
        s.studyInstitution(),
        s.studyProgramme(),
        s.studentId(),
        optionIds,
        s.consentIds(),
        s.recaptchaToken());
  }

  static RegistrationSubmission withConsents(RegistrationSubmission s, List<String> consentIds) {
    return new RegistrationSubmission(
        s.type(),
        s.firstName(),
        s.lastName(),
        s.email(),
        s.organization(),
        s.studyInstitution(),
        s.studyProgramme(),
        s.studentId(),
        s.optionIds(),
        consentIds,
        s.recaptchaToken());
  }

  static List<String> fields(ValidationResult result) {
    return result.errors().stream().map(FieldError::field).toList();
  }

  @Test
  void AC_001_01_validExternalSubmissionPassesWithOptionsInConfigurationOrder() {
    ValidationResult result = validator.validate(validExternal());

    assertThat(result.isValid()).isTrue();
    ValidatedRegistration r = result.registration();
    assertThat(r.type()).isEqualTo(RegistrationType.EXTERNAL);
    assertThat(r.options()).extracting(ConferenceOption::id).containsExactly("ws-a", "ev-d");
    assertThat(r.consentIds()).containsExactly("privacy");
    assertThat(r.recaptchaToken()).isEqualTo("token");
  }

  @Test
  void AC_001_01_noOptionsAndAnOptionalConsentAreAccepted() {
    RegistrationSubmission s =
        withConsents(withOptions(validExternal(), null), List.of("photos", "privacy"));

    ValidationResult result = validator.validate(s);

    assertThat(result.isValid()).isTrue();
    assertThat(result.registration().options()).isEmpty();
    assertThat(result.registration().consentIds()).containsExactly("privacy", "photos");
  }

  @ParameterizedTest(name = "AC-001-02 [{0}]")
  @NullSource
  @ValueSource(strings = {"", "   ", "\t\n"})
  void AC_001_02_eachRequiredExternalFieldMustNotBeBlank(String blank) {
    assertThat(fields(validator.validate(external(blank, "Novak", "a@example.com", "IJS"))))
        .containsExactly("firstName");
    assertThat(fields(validator.validate(external("Ana", blank, "a@example.com", "IJS"))))
        .containsExactly("lastName");
    assertThat(fields(validator.validate(external("Ana", "Novak", blank, "IJS"))))
        .containsExactly("email");
    assertThat(fields(validator.validate(external("Ana", "Novak", "a@example.com", blank))))
        .containsExactly("organization");
  }

  @Test
  void AC_001_02_allMissingFieldsAreReportedTogether() {
    ValidationResult result = validator.validate(external(null, "", " ", null));

    assertThat(result.isValid()).isFalse();
    assertThat(result.registration()).isNull();
    assertThat(fields(result))
        .containsExactlyInAnyOrder("firstName", "lastName", "email", "organization");
    assertThat(result.errors()).allSatisfy(e -> assertThat(e.message()).isNotBlank());
  }

  @Test
  void AC_001_03_textFieldsAreTrimmed() {
    ValidationResult result =
        validator.validate(external("  Ana ", "\tNovak\n", " ana@example.com  ", " IJS "));

    ValidatedRegistration r = result.registration();
    assertThat(r.firstName()).isEqualTo("Ana");
    assertThat(r.lastName()).isEqualTo("Novak");
    assertThat(r.email()).isEqualTo("ana@example.com");
    assertThat(r.organization()).isEqualTo("IJS");
  }

  @Test
  void AC_001_03_fieldsOfTheOtherTypeAreNotKept() {
    RegistrationSubmission s =
        new RegistrationSubmission(
            "EXTERNAL",
            "Ana",
            "Novak",
            "ana@example.com",
            "IJS",
            "Uni",
            "Prog",
            "123",
            List.of(),
            List.of("privacy"),
            "token");

    ValidatedRegistration r = validator.validate(s).registration();

    assertThat(r.studyInstitution()).isNull();
    assertThat(r.studyProgramme()).isNull();
    assertThat(r.studentId()).isNull();
  }

  @ParameterizedTest(name = "AC-001-04 [{0}]")
  @ValueSource(
      strings = {
        "ana",
        "ana@",
        "@example.com",
        "ana novak@example.com",
        "ana@@example.com",
        "ana@exa mple.com",
        "ana@localhost",
        "ana@example.com\r\nBcc: x@evil.example",
        "ana@-example.com",
        "ana@example..com"
      })
  void AC_001_04_invalidEmailIsRejected(String email) {
    assertThat(fields(validator.validate(external("Ana", "Novak", email, "IJS"))))
        .containsExactly("email");
  }

  @ParameterizedTest(name = "AC-001-04 valid [{0}]")
  @ValueSource(
      strings = {"ana@example.com", "ana.novak+konf@sub.example.si", "a_b-c@uni-lj.si", "X@Y.ZZ"})
  void AC_001_04_validEmailIsAccepted(String email) {
    assertThat(validator.validate(external("Ana", "Novak", email, "IJS")).isValid()).isTrue();
  }

  @Test
  void AC_001_04_tooLongValuesAreRejected() {
    String longName = "a".repeat(101);
    String longText = "b".repeat(201);
    String longEmail = "c".repeat(250) + "@example.com";

    assertThat(fields(validator.validate(external(longName, longName, longEmail, longText))))
        .containsExactlyInAnyOrder("firstName", "lastName", "email", "organization");
    assertThat(
            validator
                .validate(external("a".repeat(100), "Novak", "ana@example.com", "b".repeat(200)))
                .isValid())
        .isTrue();
  }

  @Test
  void AC_001_05_unknownInactiveOrDuplicateOptionsAreRejected() {
    assertThat(fields(validator.validate(withOptions(validExternal(), List.of("ws-a", "nope")))))
        .containsExactly("optionIds");
    assertThat(fields(validator.validate(withOptions(validExternal(), List.of("ws-old")))))
        .containsExactly("optionIds");
    assertThat(
            fields(
                validator.validate(withOptions(validExternal(), List.of("ev-d", "ws-a", "ev-d")))))
        .containsExactly("optionIds");
    assertThat(
            fields(validator.validate(withOptions(validExternal(), Arrays.asList("ws-a", null)))))
        .containsExactly("optionIds");
  }

  @Test
  void AC_001_06_missingMandatoryConsentIsReportedOnThatConsent() {
    assertThat(fields(validator.validate(withConsents(validExternal(), null))))
        .containsExactly("consents.privacy");
    assertThat(fields(validator.validate(withConsents(validExternal(), List.of()))))
        .containsExactly("consents.privacy");
    assertThat(fields(validator.validate(withConsents(validExternal(), List.of("photos")))))
        .containsExactly("consents.privacy");
  }

  @Test
  void AC_001_06_unknownOrRepeatedConsentIsRejected() {
    assertThat(
            fields(validator.validate(withConsents(validExternal(), List.of("privacy", "spam")))))
        .containsExactly("consentIds");
    List<String> repeated = new ArrayList<>(List.of("privacy", "privacy"));
    assertThat(fields(validator.validate(withConsents(validExternal(), repeated))))
        .containsExactly("consentIds");
  }

  @ParameterizedTest(name = "AC-001-07 token [{0}]")
  @NullSource
  @ValueSource(strings = {"", "  "})
  void AC_001_07_missingTokenIsAFieldError(String token) {
    RegistrationSubmission s =
        new RegistrationSubmission(
            "EXTERNAL",
            "Ana",
            "Novak",
            "ana@example.com",
            "IJS",
            null,
            null,
            null,
            List.of(),
            List.of("privacy"),
            token);

    assertThat(fields(validator.validate(s))).containsExactly("recaptchaToken");
  }

  @ParameterizedTest(name = "AC-001-01 type [{0}]")
  @NullSource
  @ValueSource(strings = {"", "external", "GUEST"})
  void AC_001_01_missingOrUnknownTypeIsAFieldError(String type) {
    RegistrationSubmission s =
        new RegistrationSubmission(
            type,
            "Ana",
            "Novak",
            "ana@example.com",
            "IJS",
            null,
            null,
            null,
            List.of(),
            List.of("privacy"),
            "token");

    assertThat(fields(validator.validate(s))).contains("type");
  }
}
