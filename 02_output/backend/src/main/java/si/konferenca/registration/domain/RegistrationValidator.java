package si.konferenca.registration.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Business rules BR-01 to BR-05 for a submission against the configuration. */
public class RegistrationValidator {

  static final int NAME_MAX = 100;
  static final int EMAIL_MAX = 254;
  static final int TEXT_MAX = 200;

  private static final String ATOM = "[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+";
  private static final String LABEL = "[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?";
  private static final Pattern EMAIL =
      Pattern.compile(ATOM + "(?:\\." + ATOM + ")*@" + LABEL + "(?:\\." + LABEL + ")+");

  private final ConferenceConfiguration configuration;

  public RegistrationValidator(ConferenceConfiguration configuration) {
    this.configuration = configuration;
  }

  public ValidationResult validate(RegistrationSubmission submission) {
    List<FieldError> errors = new ArrayList<>();
    RegistrationType type = type(submission.type(), errors);
    String firstName =
        required("firstName", "First name", submission.firstName(), NAME_MAX, errors);
    String lastName = required("lastName", "Last name", submission.lastName(), NAME_MAX, errors);
    String email = required("email", "Email", submission.email(), EMAIL_MAX, errors);
    if (email != null && !EMAIL.matcher(email).matches()) {
      errors.add(new FieldError("email", "Email is not a valid email address."));
      email = null;
    }
    String organization = null;
    String studyInstitution = null;
    String studyProgramme = null;
    String studentId = null;
    if (type == RegistrationType.EXTERNAL) {
      organization =
          required(
              "organization",
              "Organization / institution",
              submission.organization(),
              TEXT_MAX,
              errors);
    } else if (type == RegistrationType.STUDENT) {
      studyInstitution =
          required(
              "studyInstitution",
              "Study institution",
              submission.studyInstitution(),
              TEXT_MAX,
              errors);
      studyProgramme =
          required(
              "studyProgramme", "Study programme", submission.studyProgramme(), TEXT_MAX, errors);
      studentId = required("studentId", "Student ID", submission.studentId(), TEXT_MAX, errors);
    }
    List<ConferenceOption> options = options(submission.optionIds(), type, errors);
    List<String> consentIds = consents(submission.consentIds(), errors);
    String token = submission.recaptchaToken();
    if (token == null || token.isBlank()) {
      errors.add(new FieldError("recaptchaToken", "Please confirm that you are not a robot."));
    }
    if (!errors.isEmpty()) {
      return new ValidationResult(List.copyOf(errors), null);
    }
    return new ValidationResult(
        List.of(),
        new ValidatedRegistration(
            type,
            firstName,
            lastName,
            email,
            organization,
            studyInstitution,
            studyProgramme,
            studentId,
            options,
            consentIds,
            token));
  }

  private static RegistrationType type(String value, List<FieldError> errors) {
    if (value == null || value.isBlank()) {
      errors.add(new FieldError("type", "Registration type is required."));
      return null;
    }
    for (RegistrationType type : RegistrationType.values()) {
      if (type.name().equals(value)) {
        return type;
      }
    }
    errors.add(new FieldError("type", "Registration type is not valid."));
    return null;
  }

  private static String required(
      String field, String label, String value, int max, List<FieldError> errors) {
    String trimmed = value == null ? "" : value.strip();
    if (trimmed.isEmpty()) {
      errors.add(new FieldError(field, label + " is required."));
      return null;
    }
    if (trimmed.length() > max) {
      errors.add(new FieldError(field, label + " is longer than " + max + " characters."));
      return null;
    }
    return trimmed;
  }

  private List<ConferenceOption> options(
      List<String> optionIds, RegistrationType type, List<FieldError> errors) {
    List<String> ids = optionIds == null ? List.of() : optionIds;
    Set<String> selected = new HashSet<>();
    for (String id : ids) {
      boolean available =
          id != null
              && configuration
                  .option(id)
                  .map(o -> o.active() && (type == null || o.isAvailableTo(type)))
                  .orElse(false);
      if (!available || !selected.add(id)) {
        errors.add(
            new FieldError(
                "optionIds",
                "The selection contains an option that is not available or repeated."));
        return List.of();
      }
    }
    return configuration.options().stream().filter(o -> selected.contains(o.id())).toList();
  }

  private List<String> consents(List<String> consentIds, List<FieldError> errors) {
    List<String> ids = consentIds == null ? List.of() : consentIds;
    Set<String> given = new HashSet<>();
    for (String id : ids) {
      boolean known =
          id != null && configuration.consents().stream().anyMatch(c -> c.id().equals(id));
      if (!known || !given.add(id)) {
        errors.add(new FieldError("consentIds", "The consents contain an unknown or repeated id."));
        break;
      }
    }
    for (ConsentDefinition consent : configuration.consents()) {
      if (consent.mandatory() && !given.contains(consent.id())) {
        errors.add(new FieldError("consents." + consent.id(), "This consent is required."));
      }
    }
    return configuration.consents().stream()
        .map(ConsentDefinition::id)
        .filter(given::contains)
        .toList();
  }
}
