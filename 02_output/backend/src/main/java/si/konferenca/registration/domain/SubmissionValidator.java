package si.konferenca.registration.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/** The validation rules of spec §4 step 3 (BR-01 … BR-05, SR-04, SB-01). */
public final class SubmissionValidator {

  public static final String REQUIRED = "This field is required.";
  public static final String INVALID_EMAIL = "Enter a valid email address.";
  public static final String NOT_ALLOWED = "Not allowed for this registration type.";
  public static final String CHOOSE_TYPE = "Choose a registration type.";
  public static final String CONSENT_REQUIRED = "This consent is required.";
  public static final String UNKNOWN_CONSENT = "Unknown consent.";
  public static final String TOKEN_REQUIRED = "Please confirm that you are not a robot.";
  public static final String TOO_MANY_OPTIONS = "Too many options.";
  public static final int MAX_OPTIONS = 50;

  private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

  private final ConferenceCatalogue catalogue;

  public SubmissionValidator(ConferenceCatalogue catalogue) {
    this.catalogue = catalogue;
  }

  /** Every rule violation of a normalised submission; empty when it is valid. */
  public List<FieldError> validate(Submission s) {
    List<FieldError> errors = new ArrayList<>();
    Optional<RegistrationType> type = RegistrationType.parse(s.type());
    if (type.isEmpty()) {
      errors.add(new FieldError("type", CHOOSE_TYPE));
    }
    text(errors, "firstName", s.firstName(), 100);
    text(errors, "lastName", s.lastName(), 100);
    if (text(errors, "email", s.email(), 254) && !EMAIL.matcher(s.email()).matches()) {
      errors.add(new FieldError("email", INVALID_EMAIL));
    }
    type.ifPresent(t -> typeFields(errors, t, s));
    options(errors, type.orElse(null), s.optionIds());
    consents(errors, s.consents());
    if (isBlank(s.recaptchaToken())) {
      errors.add(new FieldError("recaptchaToken", TOKEN_REQUIRED));
    }
    return errors;
  }

  private static void typeFields(List<FieldError> errors, RegistrationType type, Submission s) {
    boolean external = type == RegistrationType.EXTERNAL;
    field(errors, external, "organization", s.organization(), 200);
    field(errors, !external, "studyInstitution", s.studyInstitution(), 200);
    field(errors, !external, "studyProgramme", s.studyProgramme(), 200);
    field(errors, !external, "studentId", s.studentId(), 50);
  }

  private static void field(
      List<FieldError> errors, boolean required, String name, String value, int max) {
    if (required) {
      text(errors, name, value, max);
    } else if (!isBlank(value)) {
      errors.add(new FieldError(name, NOT_ALLOWED));
    }
  }

  /** Checks a required text; returns whether it is present and within the length limit. */
  private static boolean text(List<FieldError> errors, String name, String value, int max) {
    if (isBlank(value)) {
      errors.add(new FieldError(name, REQUIRED));
      return false;
    }
    if (value.length() > max) {
      errors.add(new FieldError(name, "Must be at most " + max + " characters."));
      return false;
    }
    return true;
  }

  private void options(List<FieldError> errors, RegistrationType type, List<String> ids) {
    if (ids.size() > MAX_OPTIONS) {
      errors.add(new FieldError("options", TOO_MANY_OPTIONS));
      return;
    }
    Set<String> seen = new HashSet<>();
    Set<String> reported = new HashSet<>();
    for (String id : ids) {
      if (!seen.add(id)) {
        if (reported.add(id)) {
          errors.add(new FieldError("options", "Option '" + id + "' is selected more than once."));
        }
        continue;
      }
      boolean available =
          id != null
              && catalogue
                  .option(id)
                  .filter(o -> o.active() && (type == null || o.selectableBy(type)))
                  .isPresent();
      if (!available) {
        errors.add(new FieldError("options", "Option '" + id + "' is not available."));
      }
    }
  }

  private void consents(List<FieldError> errors, Map<String, Boolean> given) {
    for (String id : given.keySet()) {
      if (catalogue.consent(id).isEmpty()) {
        errors.add(new FieldError("consents." + id, UNKNOWN_CONSENT));
      }
    }
    for (ConsentDefinition consent : catalogue.consents()) {
      if (consent.mandatory() && !Boolean.TRUE.equals(given.get(consent.id()))) {
        errors.add(new FieldError("consents." + consent.id(), CONSENT_REQUIRED));
      }
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
