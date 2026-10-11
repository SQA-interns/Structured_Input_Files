package si.konferenca.registration.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Business rules for a submission (BR-01 … BR-05, SR-04; specification section 4, steps 2–7). All
 * field errors of one submission are reported together.
 */
public final class RegistrationValidator {

  static final String REQUIRED = "This field is required.";
  static final String INVALID_EMAIL = "Enter a valid email address.";
  static final String NOT_ALLOWED = "Not allowed for this registration type.";
  static final String INVALID_TYPE = "Choose external participant or student.";
  static final String UNAVAILABLE_OPTION = "One or more options are unknown or not available.";
  static final String DUPLICATE_OPTION = "An option was selected more than once.";
  static final String CONSENT_REQUIRED = "This consent is required.";
  static final String UNKNOWN_CONSENT = "Unknown consent.";

  /** No whitespace (so no line breaks, SR-05) and no address syntax characters. */
  private static final Pattern EMAIL =
      Pattern.compile(
          "^[^\\s@<>()\\[\\]\\\\,;:\"]+@[^\\s@<>()\\[\\]\\\\,;:\"]+\\.[^\\s@<>()\\[\\]\\\\,;:\"]+$");

  private static final int NAME_MAX = 100;
  private static final int EMAIL_MAX = 254;
  private static final int INSTITUTION_MAX = 200;
  private static final int STUDENT_ID_MAX = 50;

  private final ConferenceCatalog catalog;

  public RegistrationValidator(ConferenceCatalog catalog) {
    this.catalog = catalog;
  }

  /** Returns the trimmed, checked submission or throws {@link RegistrationRejectedException}. */
  public ValidSubmission validate(RegistrationSubmission s) {
    List<FieldError> errors = new ArrayList<>();
    RegistrationType type = type(s.type(), errors);
    String firstName = required("firstName", s.firstName(), NAME_MAX, errors);
    String lastName = required("lastName", s.lastName(), NAME_MAX, errors);
    String email = email(s.email(), errors);
    String organization = null;
    String studyInstitution = null;
    String studyProgramme = null;
    String studentId = null;
    if (type == RegistrationType.EXTERNAL) {
      organization = required("organization", s.organization(), INSTITUTION_MAX, errors);
      absent("studyInstitution", s.studyInstitution(), errors);
      absent("studyProgramme", s.studyProgramme(), errors);
      absent("studentId", s.studentId(), errors);
    } else if (type == RegistrationType.STUDENT) {
      absent("organization", s.organization(), errors);
      studyInstitution =
          required("studyInstitution", s.studyInstitution(), INSTITUTION_MAX, errors);
      studyProgramme = required("studyProgramme", s.studyProgramme(), INSTITUTION_MAX, errors);
      studentId = required("studentId", s.studentId(), STUDENT_ID_MAX, errors);
    }
    List<ConferenceOption> options = options(type, s.optionIds(), errors);
    List<ConsentDefinition> consents = consents(s.consentIds(), errors);
    String captchaToken = trimmed(s.captchaToken());
    if (captchaToken.isEmpty()) {
      errors.add(new FieldError("captchaToken", REQUIRED));
    }
    if (!errors.isEmpty()) {
      throw new RegistrationRejectedException(errors);
    }
    Participant participant =
        new Participant(
            firstName, lastName, email, organization, studyInstitution, studyProgramme, studentId);
    return new ValidSubmission(type, participant, options, consents, captchaToken);
  }

  private static RegistrationType type(String value, List<FieldError> errors) {
    String v = trimmed(value);
    for (RegistrationType t : RegistrationType.values()) {
      if (t.name().equals(v)) {
        return t;
      }
    }
    errors.add(new FieldError("type", INVALID_TYPE));
    return null;
  }

  private static String required(String field, String value, int max, List<FieldError> errors) {
    String v = trimmed(value);
    if (v.isEmpty()) {
      errors.add(new FieldError(field, REQUIRED));
    } else if (v.length() > max) {
      errors.add(new FieldError(field, "Use at most " + max + " characters."));
    }
    return v;
  }

  private static String email(String value, List<FieldError> errors) {
    String v = trimmed(value);
    if (v.isEmpty()) {
      errors.add(new FieldError("email", REQUIRED));
    } else if (v.length() > EMAIL_MAX || !EMAIL.matcher(v).matches()) {
      errors.add(new FieldError("email", INVALID_EMAIL));
    }
    return v;
  }

  private static void absent(String field, String value, List<FieldError> errors) {
    if (!trimmed(value).isEmpty()) {
      errors.add(new FieldError(field, NOT_ALLOWED));
    }
  }

  private List<ConferenceOption> options(
      RegistrationType type, List<String> ids, List<FieldError> errors) {
    List<String> requested = ids == null ? List.of() : ids;
    if (new HashSet<>(requested).size() != requested.size()) {
      errors.add(new FieldError("optionIds", DUPLICATE_OPTION));
      return List.of();
    }
    List<ConferenceOption> selected = new ArrayList<>();
    for (String id : requested) {
      ConferenceOption option = id == null ? null : catalog.option(id).orElse(null);
      if (option == null || type != null && !option.selectableBy(type)) {
        errors.add(new FieldError("optionIds", UNAVAILABLE_OPTION));
        return List.of();
      }
      selected.add(option);
    }
    return selected;
  }

  private List<ConsentDefinition> consents(List<String> ids, List<FieldError> errors) {
    Set<String> given = new LinkedHashSet<>(ids == null ? List.of() : ids);
    List<ConsentDefinition> result = new ArrayList<>();
    for (ConsentDefinition c : catalog.consents()) {
      if (given.remove(c.id())) {
        result.add(c);
      } else if (c.mandatory()) {
        errors.add(new FieldError("consents." + c.id(), CONSENT_REQUIRED));
      }
    }
    if (!given.isEmpty()) {
      errors.add(new FieldError("consentIds", UNKNOWN_CONSENT));
    }
    return result;
  }

  private static String trimmed(String value) {
    return value == null ? "" : value.strip();
  }
}
