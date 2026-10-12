package si.konferenca.registration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import si.konferenca.registration.conference.Category;
import si.konferenca.registration.conference.ConferenceConfiguration;
import si.konferenca.registration.conference.ConferenceOption;
import si.konferenca.registration.conference.ConsentDefinition;
import si.konferenca.registration.domain.ParticipantDetails;
import si.konferenca.registration.domain.RegistrationType;

class RegistrationValidatorTest {

  static final ConferenceOption WORKSHOP =
      new ConferenceOption(
          "ws", "Workshop", Category.WORKSHOP, true, EnumSet.allOf(RegistrationType.class));
  static final ConferenceOption DINNER =
      new ConferenceOption(
          "dinner", "Dinner", Category.MEAL, true, EnumSet.of(RegistrationType.EXTERNAL));
  static final ConferenceOption OLD =
      new ConferenceOption(
          "old", "Old", Category.EVENT, false, EnumSet.allOf(RegistrationType.class));
  static final ConsentDefinition PRIVACY = new ConsentDefinition("privacy", "Privacy", true);
  static final ConsentDefinition PHOTO = new ConsentDefinition("photo", "Photo", false);

  private final RegistrationValidator validator =
      new RegistrationValidator(
          new ConferenceConfiguration(List.of(WORKSHOP, DINNER, OLD), List.of(PRIVACY, PHOTO)));

  static RegistrationCommand external(String... optionIds) {
    return new RegistrationCommand(
        "EXTERNAL",
        " Ana ",
        "Novak",
        "ana@example.org ",
        "Institut",
        null,
        null,
        null,
        Arrays.asList(optionIds),
        List.of("privacy"),
        "token",
        "1.2.3.4");
  }

  static RegistrationCommand student(String... optionIds) {
    return new RegistrationCommand(
        "STUDENT",
        "Luka",
        "Kranjc",
        "luka@example.org",
        null,
        "Univerza",
        "CS",
        "6320",
        Arrays.asList(optionIds),
        List.of("privacy", "photo"),
        "token",
        "1.2.3.4");
  }

  private List<FieldError> errors(RegistrationCommand command) {
    try {
      validator.validate(command);
      return List.of();
    } catch (RegistrationRejectedException e) {
      return e.fieldErrors();
    }
  }

  @Test
  void acceptsAndTrimsAnExternalSubmission() {
    ValidatedRegistration valid = validator.validate(external("ws", "dinner"));

    assertThat(valid.type()).isEqualTo(RegistrationType.EXTERNAL);
    assertThat(valid.participant())
        .isEqualTo(
            new ParticipantDetails(
                "Ana", "Novak", "ana@example.org", "Institut", null, null, null));
    assertThat(valid.options()).containsExactly(WORKSHOP, DINNER);
    assertThat(valid.consents()).containsExactly(PRIVACY);
  }

  @Test
  void acceptsAStudentSubmission() {
    ValidatedRegistration valid = validator.validate(student("ws"));

    assertThat(valid.type()).isEqualTo(RegistrationType.STUDENT);
    assertThat(valid.participant())
        .isEqualTo(
            new ParticipantDetails(
                "Luka", "Kranjc", "luka@example.org", null, "Univerza", "CS", "6320"));
    assertThat(valid.consents()).containsExactly(PRIVACY, PHOTO);
  }

  @Test
  void acceptsMissingOptionListAndPaddedType() {
    RegistrationCommand command =
        new RegistrationCommand(
            " STUDENT ",
            "Luka",
            "Kranjc",
            "luka@example.org",
            "",
            "U",
            "P",
            "1",
            null,
            List.of("privacy"),
            "t",
            null);

    assertThat(validator.validate(command).options()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"GUEST", "external", ""})
  void rejectsUnknownType(String type) {
    RegistrationCommand command =
        new RegistrationCommand(
            type,
            "A",
            "B",
            "a@b.org",
            "O",
            null,
            null,
            null,
            List.of("ws"),
            List.of("privacy"),
            "t",
            null);

    assertThat(errors(command))
        .containsExactly(new FieldError("type", RegistrationValidator.INVALID_TYPE));
  }

  @Test
  void rejectsMissingTypeAndStillChecksConsentsAndToken() {
    RegistrationCommand command =
        new RegistrationCommand(
            null, null, null, null, null, null, null, null, null, null, null, null);

    assertThat(errors(command))
        .containsExactly(
            new FieldError("type", RegistrationValidator.INVALID_TYPE),
            new FieldError("consents.privacy", RegistrationValidator.CONSENT_REQUIRED),
            new FieldError("recaptchaToken", RegistrationValidator.TOKEN_REQUIRED));
  }

  @Test
  void reportsEveryMissingFieldOfTheType() {
    RegistrationCommand command =
        new RegistrationCommand(
            "STUDENT",
            " ",
            null,
            "",
            null,
            null,
            "\t",
            null,
            List.of(),
            List.of("privacy"),
            " ",
            null);

    assertThat(errors(command))
        .containsExactly(
            new FieldError("firstName", RegistrationValidator.REQUIRED),
            new FieldError("lastName", RegistrationValidator.REQUIRED),
            new FieldError("email", RegistrationValidator.REQUIRED),
            new FieldError("studyInstitution", RegistrationValidator.REQUIRED),
            new FieldError("studyProgramme", RegistrationValidator.REQUIRED),
            new FieldError("studentId", RegistrationValidator.REQUIRED),
            new FieldError("recaptchaToken", RegistrationValidator.TOKEN_REQUIRED));
  }

  @Test
  void rejectsFieldsOfTheOtherType() {
    RegistrationCommand external =
        new RegistrationCommand(
            "EXTERNAL",
            "A",
            "B",
            "a@b.org",
            "O",
            "U",
            " ",
            "1",
            List.of(),
            List.of("privacy"),
            "t",
            null);
    RegistrationCommand student =
        new RegistrationCommand(
            "STUDENT",
            "A",
            "B",
            "a@b.org",
            "Org",
            "U",
            "P",
            "1",
            List.of(),
            List.of("privacy"),
            "t",
            null);

    assertThat(errors(external))
        .containsExactly(
            new FieldError("studyInstitution", RegistrationValidator.NOT_FOR_TYPE),
            new FieldError("studentId", RegistrationValidator.NOT_FOR_TYPE));
    assertThat(errors(student))
        .containsExactly(new FieldError("organization", RegistrationValidator.NOT_FOR_TYPE));
  }

  @Test
  void enforcesMaximumLengths() {
    RegistrationCommand atLimit =
        new RegistrationCommand(
            "STUDENT",
            "a".repeat(100),
            "b".repeat(100),
            "a".repeat(240) + "@example.org",
            null,
            "c".repeat(200),
            "d".repeat(200),
            "e".repeat(50),
            List.of(),
            List.of("privacy"),
            "t",
            null);
    RegistrationCommand overLimit =
        new RegistrationCommand(
            "STUDENT",
            "a".repeat(101),
            "b".repeat(101),
            "a".repeat(243) + "@example.org",
            null,
            "c".repeat(201),
            "d".repeat(201),
            "e".repeat(51),
            List.of(),
            List.of("privacy"),
            "t",
            null);
    RegistrationCommand longOrganization =
        new RegistrationCommand(
            "EXTERNAL",
            "A",
            "B",
            "a@b.org",
            "o".repeat(201),
            null,
            null,
            null,
            List.of(),
            List.of("privacy"),
            "t",
            null);

    assertThat(errors(atLimit)).isEmpty();
    assertThat(errors(overLimit))
        .extracting(FieldError::field)
        .containsExactly(
            "firstName", "lastName", "email", "studyInstitution", "studyProgramme", "studentId");
    assertThat(errors(overLimit))
        .extracting(FieldError::message)
        .containsOnly(RegistrationValidator.TOO_LONG);
    assertThat(errors(longOrganization))
        .containsExactly(new FieldError("organization", RegistrationValidator.TOO_LONG));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "ana",
        "ana@example",
        "a b@example.org",
        "ana@exa mple.org",
        "ana@@example.org",
        "@example.org",
        "ana@.org",
        "ana@example.org.",
        "ana..x@example.org",
        "\"ana@x\"@example.org"
      })
  void rejectsInvalidEmails(String email) {
    assertThat(RegistrationValidator.isValidEmail(email)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"ana@example.org", "a.b+c@sub.example.co.uk", "x_y@example.si"})
  void acceptsValidEmails(String email) {
    assertThat(RegistrationValidator.isValidEmail(email)).isTrue();
  }

  @Test
  void reportsInvalidEmailOnTheEmailField() {
    RegistrationCommand command =
        new RegistrationCommand(
            "EXTERNAL",
            "A",
            "B",
            "not-an-email",
            "O",
            null,
            null,
            null,
            List.of(),
            List.of("privacy"),
            "t",
            null);

    assertThat(errors(command))
        .containsExactly(new FieldError("email", RegistrationValidator.INVALID_EMAIL));
  }

  @Test
  void rejectsUnknownInactiveUnavailableNullAndRepeatedOptions() {
    for (RegistrationCommand command :
        List.of(
            external("ws", "nope"),
            external("old"),
            external("ws", "ws"),
            external("ws", null),
            student("dinner"))) {
      assertThat(errors(command))
          .as(command.optionIds().toString())
          .containsExactly(new FieldError("optionIds", RegistrationValidator.INVALID_OPTIONS));
    }
  }

  @Test
  void checksConsents() {
    RegistrationCommand onlyOptional =
        new RegistrationCommand(
            "EXTERNAL",
            "A",
            "B",
            "a@b.org",
            "O",
            null,
            null,
            null,
            List.of(),
            List.of("photo"),
            "t",
            null);
    RegistrationCommand unknown =
        new RegistrationCommand(
            "EXTERNAL",
            "A",
            "B",
            "a@b.org",
            "O",
            null,
            null,
            null,
            List.of(),
            List.of("privacy", "marketing"),
            "t",
            null);
    RegistrationCommand repeated =
        new RegistrationCommand(
            "EXTERNAL",
            "A",
            "B",
            "a@b.org",
            "O",
            null,
            null,
            null,
            List.of(),
            List.of("privacy", "privacy"),
            "t",
            null);

    assertThat(errors(onlyOptional))
        .containsExactly(
            new FieldError("consents.privacy", RegistrationValidator.CONSENT_REQUIRED));
    assertThat(errors(unknown))
        .containsExactly(new FieldError("consents", RegistrationValidator.UNKNOWN_CONSENT));
    assertThat(errors(repeated)).isEmpty();
  }

  @Test
  void rejectedExceptionCopiesTheErrors() {
    List<FieldError> errors = new java.util.ArrayList<>(List.of(new FieldError("a", "b")));
    RegistrationRejectedException exception = new RegistrationRejectedException(errors);
    errors.clear();

    assertThat(exception.fieldErrors()).containsExactly(new FieldError("a", "b"));
    assertThatThrownBy(() -> exception.fieldErrors().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
