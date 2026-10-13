package si.konferenca.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RegistrationValidatorTest {

  static final ConferenceOption WORKSHOP =
      new ConferenceOption(
          "ws", "Workshop", OptionCategory.WORKSHOP, true, EnumSet.allOf(RegistrationType.class));
  static final ConferenceOption EXTERNAL_ONLY =
      new ConferenceOption(
          "ext",
          "External only",
          OptionCategory.EVENT,
          true,
          EnumSet.of(RegistrationType.EXTERNAL));
  static final ConferenceOption INACTIVE =
      new ConferenceOption(
          "old", "Old", OptionCategory.MEAL, false, EnumSet.allOf(RegistrationType.class));
  static final ConsentDefinition PRIVACY = new ConsentDefinition("privacy", "Privacy", true);
  static final ConsentDefinition PHOTOS = new ConsentDefinition("photos", "Photos", false);
  static final ConferenceCatalog CATALOG =
      new ConferenceCatalog(List.of(WORKSHOP, EXTERNAL_ONLY, INACTIVE), List.of(PRIVACY, PHOTOS));

  private final RegistrationValidator validator = new RegistrationValidator(CATALOG);

  static RegistrationSubmission external(List<String> options, List<String> consents) {
    return new RegistrationSubmission(
        "EXTERNAL",
        " Ana ",
        "Novak",
        " ana@example.com ",
        "Acme",
        null,
        "",
        "  ",
        options,
        consents);
  }

  static RegistrationSubmission student(String institution, String programme, String id) {
    return new RegistrationSubmission(
        "STUDENT",
        "Bor",
        "Kranjc",
        "bor@example.com",
        null,
        institution,
        programme,
        id,
        List.of("ws"),
        List.of("privacy"));
  }

  private Map<String, String> errors(RegistrationSubmission submission) {
    RegistrationValidator.Result result = validator.validate(submission);
    assertThat(result.valid()).isEqualTo(result.errors().isEmpty());
    return result.errors();
  }

  @Test
  void validExternalSubmissionIsTrimmedAndKeepsOptionsAndGivenConsents() {
    RegistrationValidator.Result result =
        validator.validate(external(List.of("ext", "ws"), List.of("photos", "privacy")));

    assertThat(result.valid()).isTrue();
    assertThat(result.errors()).isEmpty();
    ValidatedRegistration r = result.registration();
    assertThat(r.type()).isEqualTo(RegistrationType.EXTERNAL);
    assertThat(r.firstName()).isEqualTo("Ana");
    assertThat(r.lastName()).isEqualTo("Novak");
    assertThat(r.email()).isEqualTo("ana@example.com");
    assertThat(r.organization()).isEqualTo("Acme");
    assertThat(r.studyInstitution()).isNull();
    assertThat(r.studyProgramme()).isNull();
    assertThat(r.studentId()).isNull();
    assertThat(r.options()).containsExactly(EXTERNAL_ONLY, WORKSHOP);
    assertThat(r.consents()).containsExactly(PRIVACY, PHOTOS);
  }

  @Test
  void validStudentSubmissionHasStudentFieldsOnly() {
    RegistrationValidator.Result result = validator.validate(student(" Uni ", "CS", "123"));

    assertThat(result.valid()).isTrue();
    ValidatedRegistration r = result.registration();
    assertThat(r.type()).isEqualTo(RegistrationType.STUDENT);
    assertThat(r.organization()).isNull();
    assertThat(r.studyInstitution()).isEqualTo("Uni");
    assertThat(r.studyProgramme()).isEqualTo("CS");
    assertThat(r.studentId()).isEqualTo("123");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "OTHER", "external", "EXTERNAL "})
  void unknownTypeIsAFieldErrorAndOtherFieldsAreStillChecked(String type) {
    Map<String, String> errors =
        errors(
            new RegistrationSubmission(
                type, "", "N", "bad", "Org", "Uni", null, null, List.of("ws"), List.of("privacy")));

    assertThat(errors)
        .containsEntry("type", RegistrationValidator.CHOOSE_TYPE)
        .containsEntry("firstName", RegistrationValidator.REQUIRED)
        .containsEntry("email", RegistrationValidator.INVALID_EMAIL)
        .doesNotContainKeys("organization", "studyInstitution", "optionIds");
  }

  @Test
  void missingTypeStillRejectsInactiveOptions() {
    Map<String, String> errors =
        errors(
            new RegistrationSubmission(
                null, "A", "B", "a@b.co", null, null, null, null, List.of("old"), List.of()));

    assertThat(errors)
        .containsEntry("type", RegistrationValidator.CHOOSE_TYPE)
        .containsEntry("optionIds", RegistrationValidator.INVALID_OPTIONS)
        .containsEntry("consents.privacy", RegistrationValidator.CONSENT_REQUIRED);
  }

  @Test
  void lengthLimitsAreInclusive() {
    String hundred = "a".repeat(100);
    String twoHundred = "o".repeat(200);
    RegistrationSubmission atLimit =
        new RegistrationSubmission(
            "EXTERNAL",
            hundred,
            hundred,
            "a@b.co",
            twoHundred,
            null,
            null,
            null,
            null,
            List.of("privacy"));
    assertThat(errors(atLimit)).isEmpty();

    RegistrationSubmission over =
        new RegistrationSubmission(
            "EXTERNAL",
            hundred + "a",
            hundred + "b",
            "a@b.co",
            twoHundred + "o",
            null,
            null,
            null,
            null,
            List.of("privacy"));
    assertThat(errors(over))
        .containsEntry("firstName", "Use at most 100 characters.")
        .containsEntry("lastName", "Use at most 100 characters.")
        .containsEntry("organization", "Use at most 200 characters.");
  }

  @Test
  void studentLengthLimits() {
    assertThat(errors(student("u".repeat(200), "p".repeat(200), "1".repeat(50)))).isEmpty();
    assertThat(errors(student("u".repeat(201), "p".repeat(201), "1".repeat(51))))
        .containsEntry("studyInstitution", "Use at most 200 characters.")
        .containsEntry("studyProgramme", "Use at most 200 characters.")
        .containsEntry("studentId", "Use at most 50 characters.");
  }

  @Test
  void emailLengthLimitIs254() {
    String local = "a".repeat(64);
    String domain = "d".repeat(254 - 64 - 1 - 3) + ".si";
    String email = local + "@" + domain;
    assertThat(email).hasSize(254);
    RegistrationSubmission ok =
        new RegistrationSubmission(
            "EXTERNAL", "A", "B", email, "Org", null, null, null, null, List.of("privacy"));
    assertThat(errors(ok)).isEmpty();

    RegistrationSubmission tooLong =
        new RegistrationSubmission(
            "EXTERNAL", "A", "B", "x" + email, "Org", null, null, null, null, List.of("privacy"));
    assertThat(errors(tooLong)).containsEntry("email", RegistrationValidator.INVALID_EMAIL);
  }

  @ParameterizedTest
  @ValueSource(strings = {"   ", "\t"})
  void blankEmailIsRequiredNotInvalid(String email) {
    RegistrationSubmission s =
        new RegistrationSubmission(
            "EXTERNAL", "A", "B", email, "Org", null, null, null, null, List.of("privacy"));
    assertThat(errors(s)).containsEntry("email", RegistrationValidator.REQUIRED);
  }

  @Test
  void fieldsOfTheOtherTypeMustBeEmpty() {
    RegistrationSubmission externalWithStudentFields =
        new RegistrationSubmission(
            "EXTERNAL", "A", "B", "a@b.co", "Org", "Uni", "Prog", "1", null, List.of("privacy"));
    assertThat(errors(externalWithStudentFields))
        .containsEntry("studyInstitution", RegistrationValidator.NOT_APPLICABLE)
        .containsEntry("studyProgramme", RegistrationValidator.NOT_APPLICABLE)
        .containsEntry("studentId", RegistrationValidator.NOT_APPLICABLE)
        .doesNotContainKey("organization");

    RegistrationSubmission studentWithOrganization =
        new RegistrationSubmission(
            "STUDENT", "A", "B", "a@b.co", "Org", "Uni", "Prog", "1", null, List.of("privacy"));
    assertThat(errors(studentWithOrganization))
        .containsOnlyKeys("organization")
        .containsEntry("organization", RegistrationValidator.NOT_APPLICABLE);
  }

  @Test
  void optionRules() {
    assertThat(errors(external(null, List.of("privacy")))).isEmpty();
    assertThat(validator.validate(external(null, List.of("privacy"))).registration().options())
        .isEmpty();
    assertThat(errors(external(List.of("ws", "ws"), List.of("privacy"))))
        .containsEntry("optionIds", RegistrationValidator.INVALID_OPTIONS);
    assertThat(errors(external(List.of("nope"), List.of("privacy"))))
        .containsEntry("optionIds", RegistrationValidator.INVALID_OPTIONS);
    assertThat(errors(external(List.of("old"), List.of("privacy"))))
        .containsEntry("optionIds", RegistrationValidator.INVALID_OPTIONS);
    assertThat(errors(external(Arrays.asList("ws", null), List.of("privacy"))))
        .containsEntry("optionIds", RegistrationValidator.INVALID_OPTIONS);
    RegistrationSubmission studentPickingExternalOnly =
        new RegistrationSubmission(
            "STUDENT", "A", "B", "a@b.co", null, "U", "P", "1", List.of("ext"), List.of("privacy"));
    assertThat(errors(studentPickingExternalOnly)).containsOnlyKeys("optionIds");
  }

  @Test
  void atMostOneHundredOptionIds() {
    List<ConferenceOption> many = new ArrayList<>();
    List<String> ids = new ArrayList<>();
    for (int i = 0; i < 101; i++) {
      many.add(
          new ConferenceOption(
              "o" + i, "O" + i, OptionCategory.OTHER, true, EnumSet.allOf(RegistrationType.class)));
      ids.add("o" + i);
    }
    RegistrationValidator big =
        new RegistrationValidator(new ConferenceCatalog(many, List.of(PRIVACY)));

    assertThat(big.validate(external(ids.subList(0, 100), List.of("privacy"))).valid()).isTrue();
    assertThat(big.validate(external(ids, List.of("privacy"))).errors())
        .containsEntry("optionIds", RegistrationValidator.INVALID_OPTIONS);
  }

  @Test
  void consentRules() {
    assertThat(errors(external(null, null)))
        .containsEntry("consents.privacy", RegistrationValidator.CONSENT_REQUIRED)
        .doesNotContainKey("consents.photos");
    assertThat(errors(external(null, List.of("privacy", "privacy")))).isEmpty();
    assertThat(errors(external(null, List.of("privacy", "unknown"))))
        .containsOnlyKeys("consents")
        .containsEntry("consents", RegistrationValidator.UNKNOWN_CONSENT);
    assertThat(errors(external(null, Arrays.asList("privacy", null))))
        .containsEntry("consents", RegistrationValidator.UNKNOWN_CONSENT);
  }

  @Test
  void acceptGivesIdTimeOptionsAndConsentsGivenAtThatTime() {
    ValidatedRegistration validated =
        validator.validate(external(List.of("ws"), List.of("privacy"))).registration();
    UUID id = UUID.randomUUID();
    Instant at = Instant.parse("2026-10-13T08:00:00.123Z");

    Registration r = validated.accept(id, at);

    assertThat(r.id()).isEqualTo(id);
    assertThat(r.registeredAt()).isEqualTo(at);
    assertThat(r.type()).isEqualTo(RegistrationType.EXTERNAL);
    assertThat(r.firstName()).isEqualTo("Ana");
    assertThat(r.lastName()).isEqualTo("Novak");
    assertThat(r.email()).isEqualTo("ana@example.com");
    assertThat(r.organization()).isEqualTo("Acme");
    assertThat(r.options())
        .containsExactly(
            new Registration.SelectedOption("ws", "Workshop", OptionCategory.WORKSHOP));
    assertThat(r.consents())
        .containsExactly(new Registration.GivenConsent("privacy", "Privacy", at));
  }
}
