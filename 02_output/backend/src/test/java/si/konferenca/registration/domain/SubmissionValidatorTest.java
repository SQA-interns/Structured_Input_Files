package si.konferenca.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SubmissionValidatorTest {

  private static final ConferenceCatalogue CATALOGUE =
      new ConferenceCatalogue(
          List.of(
              new ConferenceOption(
                  "both",
                  "Both",
                  OptionCategory.WORKSHOP,
                  true,
                  EnumSet.allOf(RegistrationType.class)),
              new ConferenceOption(
                  "ext",
                  "External only",
                  OptionCategory.EVENT,
                  true,
                  EnumSet.of(RegistrationType.EXTERNAL)),
              new ConferenceOption(
                  "off",
                  "Inactive",
                  OptionCategory.MEAL,
                  false,
                  EnumSet.allOf(RegistrationType.class))),
          List.of(
              new ConsentDefinition("privacy", "Privacy", true),
              new ConsentDefinition("photos", "Photos", false)));

  private final SubmissionValidator validator = new SubmissionValidator(CATALOGUE);

  private static Submission external(Map<String, Object> overrides) {
    Map<String, Object> v = new HashMap<>();
    v.put("type", "EXTERNAL");
    v.put("firstName", "Ana");
    v.put("lastName", "Novak");
    v.put("email", "ana@example.si");
    v.put("organization", "Org");
    v.put("optionIds", List.of("both"));
    v.put("consents", Map.of("privacy", true));
    v.put("recaptchaToken", "token");
    v.putAll(overrides);
    return submission(v);
  }

  @SuppressWarnings("unchecked")
  private static Submission submission(Map<String, Object> v) {
    return new Submission(
            (String) v.get("type"),
            (String) v.get("firstName"),
            (String) v.get("lastName"),
            (String) v.get("email"),
            (String) v.get("organization"),
            (String) v.get("studyInstitution"),
            (String) v.get("studyProgramme"),
            (String) v.get("studentId"),
            (List<String>) v.get("optionIds"),
            (Map<String, Boolean>) v.get("consents"),
            (String) v.get("recaptchaToken"))
        .normalized();
  }

  private List<FieldError> validate(Map<String, Object> overrides) {
    return validator.validate(external(overrides));
  }

  @Test
  void validExternalSubmissionHasNoErrors() {
    assertThat(validate(Map.of())).isEmpty();
  }

  @Test
  void validStudentSubmissionHasNoErrors() {
    Map<String, Object> v = new HashMap<>();
    v.put("type", "STUDENT");
    v.put("organization", null);
    v.put("studyInstitution", "Uni");
    v.put("studyProgramme", "CS");
    v.put("studentId", "1");
    assertThat(validate(v)).isEmpty();
  }

  @Test
  void unknownTypeIsReportedAndTypeFieldsAreNotChecked() {
    assertThat(validate(Map.of("type", "VISITOR")))
        .containsExactly(new FieldError("type", SubmissionValidator.CHOOSE_TYPE));
  }

  @Test
  void textAtTheLengthLimitIsAcceptedAndOneMoreIsRejected() {
    assertThat(validate(Map.of("firstName", "a".repeat(100)))).isEmpty();
    assertThat(validate(Map.of("firstName", "a".repeat(101))))
        .containsExactly(new FieldError("firstName", "Must be at most 100 characters."));
    assertThat(validate(Map.of("organization", "o".repeat(201))))
        .containsExactly(new FieldError("organization", "Must be at most 200 characters."));
  }

  @Test
  void overlongEmailIsReportedOnlyAsTooLong() {
    String email = "a".repeat(250) + "@x.si";
    assertThat(validate(Map.of("email", email)))
        .containsExactly(new FieldError("email", "Must be at most 254 characters."));
  }

  @Test
  void fieldOfTheOtherTypeIsNotAllowed() {
    assertThat(validate(Map.of("studentId", "123")))
        .containsExactly(new FieldError("studentId", SubmissionValidator.NOT_ALLOWED));
    assertThat(validate(Map.of("studentId", "   "))).isEmpty();
  }

  @Test
  void optionNotAvailableToTheTypeIsRejected() {
    Map<String, Object> v = new HashMap<>();
    v.put("type", "STUDENT");
    v.put("organization", null);
    v.put("studyInstitution", "Uni");
    v.put("studyProgramme", "CS");
    v.put("studentId", "1");
    v.put("optionIds", List.of("ext"));
    assertThat(validate(v))
        .containsExactly(new FieldError("options", "Option 'ext' is not available."));
  }

  @Test
  void inactiveOptionIsRejectedEvenWithoutAValidType() {
    List<FieldError> errors = validate(Map.of("type", "", "optionIds", List.of("off", "ext")));
    assertThat(errors)
        .contains(new FieldError("options", "Option 'off' is not available."))
        .doesNotContain(new FieldError("options", "Option 'ext' is not available."));
  }

  @Test
  void duplicateOptionIsReportedOnce() {
    assertThat(validate(Map.of("optionIds", List.of("both", "both", "both"))))
        .containsExactly(new FieldError("options", "Option 'both' is selected more than once."));
  }

  @Test
  void nullOptionIdIsNotAvailable() {
    List<String> ids = new ArrayList<>();
    ids.add(null);
    assertThat(validate(Map.of("optionIds", ids)))
        .containsExactly(new FieldError("options", "Option 'null' is not available."));
  }

  @Test
  void fiftyOptionsAreCheckedAndFiftyOneAreTooMany() {
    List<String> fifty = new ArrayList<>();
    for (int i = 0; i < 50; i++) {
      fifty.add("x" + i);
    }
    assertThat(validate(Map.of("optionIds", fifty))).hasSize(50);
    fifty.add("x50");
    assertThat(validate(Map.of("optionIds", fifty)))
        .containsExactly(new FieldError("options", SubmissionValidator.TOO_MANY_OPTIONS));
  }

  @Test
  void unknownConsentAndMissingMandatoryConsentAreReported() {
    assertThat(validate(Map.of("consents", Map.of("newsletter", true))))
        .containsExactlyInAnyOrder(
            new FieldError("consents.newsletter", SubmissionValidator.UNKNOWN_CONSENT),
            new FieldError("consents.privacy", SubmissionValidator.CONSENT_REQUIRED));
  }

  @Test
  void optionalConsentMayBeMissing() {
    assertThat(validate(Map.of("consents", Map.of("privacy", true, "photos", false)))).isEmpty();
  }

  @Test
  void blankTokenIsReported() {
    assertThat(validate(Map.of("recaptchaToken", " ")))
        .containsExactly(new FieldError("recaptchaToken", SubmissionValidator.TOKEN_REQUIRED));
  }

  @Test
  void normalizedReplacesNullCollectionsAndStripsText() {
    Submission s =
        new Submission(" EXTERNAL ", " a ", null, null, null, null, null, null, null, null, null)
            .normalized();
    assertThat(s.type()).isEqualTo("EXTERNAL");
    assertThat(s.firstName()).isEqualTo("a");
    assertThat(s.optionIds()).isEmpty();
    assertThat(s.consents()).isEmpty();
  }

  @Test
  void emailRule() {
    assertThat(SubmissionValidator.isEmail("a@b.si")).isTrue();
    assertThat(SubmissionValidator.isEmail("a@b")).isFalse();
    assertThat(SubmissionValidator.isEmail("a b@c.si")).isFalse();
    assertThat(SubmissionValidator.isEmail(null)).isFalse();
    assertThat(SubmissionValidator.isEmail("a".repeat(250) + "@b.si")).isFalse();
    assertThat(SubmissionValidator.isEmail("a".repeat(249) + "@b.si")).isTrue();
  }
}
