package si.konferenca.registration.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/** Server-side field rules and normalisation (BR-01..BR-05, SB-01, SR-04; specification S4). */
public final class RegistrationValidator {

  static final int MAX_TEXT = 200;
  static final int MAX_EMAIL = 254;
  static final String REQUIRED = "This field is required.";
  static final String NOT_ALLOWED = "Not allowed for this registration type.";
  static final String BAD_CHARACTERS = "Contains characters that are not allowed.";
  static final String BAD_EMAIL = "Enter a valid email address.";
  static final String CONSENT_REQUIRED = "Consent is required.";

  private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
  private static final Pattern OPTION_ID = Pattern.compile("^[a-z0-9][a-z0-9-]{0,63}$");

  private final OptionCatalog catalog;

  public RegistrationValidator(OptionCatalog catalog) {
    this.catalog = catalog;
  }

  /**
   * Validates the form and returns the accepted registration.
   *
   * @throws ValidationException with every field error found
   */
  public Registration validate(RegistrationForm form, UUID id, Instant now) {
    List<FieldError> errors = new ArrayList<>();
    RegistrationType type = type(form.type(), errors);
    String firstName = text("firstName", form.firstName(), errors);
    String lastName = text("lastName", form.lastName(), errors);
    String email = email(form.email(), errors);
    boolean external = type == RegistrationType.EXTERNAL;
    boolean student = type == RegistrationType.STUDENT;
    String organization = typed("organization", form.organization(), external, type, errors);
    String institution = typed("studyInstitution", form.studyInstitution(), student, type, errors);
    String programme = typed("studyProgramme", form.studyProgramme(), student, type, errors);
    String studentId = typed("studentId", form.studentId(), student, type, errors);
    List<SelectedOption> options = options(form.optionIds(), type, errors);
    if (!Boolean.TRUE.equals(form.consentGiven())) {
      errors.add(new FieldError("consentGiven", CONSENT_REQUIRED));
    }
    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }
    Consent consent = catalog.consent();
    return new Registration(
        id,
        type,
        now,
        firstName,
        lastName,
        email,
        organization,
        institution,
        programme,
        studentId,
        options,
        new ConsentRecord(consent.id(), consent.text(), now));
  }

  private static RegistrationType type(String value, List<FieldError> errors) {
    for (RegistrationType type : RegistrationType.values()) {
      if (type.name().equals(value)) {
        return type;
      }
    }
    errors.add(new FieldError("type", "Choose a registration type."));
    return null;
  }

  /** Trimmed value of a field that is required for one type and forbidden for the other. */
  private static String typed(
      String field, String value, boolean belongs, RegistrationType type, List<FieldError> errors) {
    if (type == null) {
      return trimToNull(value);
    }
    if (belongs) {
      return text(field, value, errors);
    }
    if (trimToNull(value) != null) {
      errors.add(new FieldError(field, NOT_ALLOWED));
    }
    return null;
  }

  private static String text(String field, String value, List<FieldError> errors) {
    String trimmed = trimToNull(value);
    if (trimmed == null) {
      errors.add(new FieldError(field, REQUIRED));
      return null;
    }
    if (trimmed.length() > MAX_TEXT) {
      errors.add(new FieldError(field, "Must be at most " + MAX_TEXT + " characters."));
    } else if (hasControlCharacter(trimmed, true)) {
      errors.add(new FieldError(field, BAD_CHARACTERS));
    }
    return trimmed;
  }

  private static String email(String value, List<FieldError> errors) {
    String trimmed = trimToNull(value);
    if (trimmed == null) {
      errors.add(new FieldError("email", REQUIRED));
      return null;
    }
    if (trimmed.length() > MAX_EMAIL) {
      errors.add(new FieldError("email", "Must be at most " + MAX_EMAIL + " characters."));
    } else if (hasControlCharacter(trimmed, false) || !EMAIL.matcher(trimmed).matches()) {
      errors.add(new FieldError("email", BAD_EMAIL));
    }
    return trimmed;
  }

  private List<SelectedOption> options(
      List<String> ids, RegistrationType type, List<FieldError> errors) {
    List<SelectedOption> selected = new ArrayList<>();
    if (ids == null) {
      errors.add(new FieldError("optionIds", REQUIRED));
      return selected;
    }
    Set<String> seen = new HashSet<>();
    for (String id : ids) {
      String shown = id != null && OPTION_ID.matcher(id).matches() ? id : null;
      if (!seen.add(id)) {
        errors.add(new FieldError("optionIds", "Option selected more than once: " + shown));
        continue;
      }
      ConferenceOption option = id == null ? null : catalog.find(id).orElse(null);
      if (option == null || type != null && !option.selectableBy(type)) {
        errors.add(
            new FieldError(
                "optionIds",
                shown == null
                    ? "Unknown option."
                    : "Unknown, inactive or unavailable option: " + shown));
        continue;
      }
      selected.add(new SelectedOption(option.id(), option.name(), option.category()));
    }
    return selected;
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.strip();
    return trimmed.isEmpty() ? null : trimmed;
  }

  /** Control characters are refused; tab, CR and LF only inside free text (AC-006-03). */
  private static boolean hasControlCharacter(String value, boolean allowLineBreaks) {
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      boolean control = c < 0x20 || c == 0x7f;
      boolean lineBreak = c == '\t' || c == '\r' || c == '\n';
      if (control && !(allowLineBreaks && lineBreak)) {
        return true;
      }
    }
    return false;
  }
}
