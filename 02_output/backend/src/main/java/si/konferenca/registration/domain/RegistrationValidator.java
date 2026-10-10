package si.konferenca.registration.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Server-side validation of a registration form (SB-01): required fields per type (BR-01, BR-02),
 * email format (BR-03), length and control characters (D-14, SR-05), options (BR-04, SR-04, D-08,
 * D-11) and consents (BR-05).
 */
public final class RegistrationValidator {

  public static final int MAX_TEXT = 200;
  public static final int MAX_EMAIL = 254;
  private static final int MAX_LOCAL_PART = 64;
  private static final String ATOM = "[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+";
  private static final Pattern LOCAL_PART = Pattern.compile(ATOM + "(\\." + ATOM + ")*");
  private static final Pattern DOMAIN =
      Pattern.compile("([A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?\\.)+[A-Za-z]{2,}");

  private final OptionCatalog catalog;

  public RegistrationValidator(OptionCatalog catalog) {
    this.catalog = catalog;
  }

  /**
   * Returns the validated registration.
   *
   * @throws ValidationException with every field error found
   */
  public ValidRegistration validate(RegistrationForm form) {
    List<FieldError> errors = new ArrayList<>();
    Optional<RegistrationType> type = type(form.type(), errors);
    String firstName = text("firstName", form.firstName(), errors);
    String lastName = text("lastName", form.lastName(), errors);
    String email = email(form.email(), errors);
    String organization = null;
    if (type.isPresent() && type.get() == RegistrationType.EXTERNAL) {
      organization = text("organization", form.organization(), errors);
      notAllowed("studyInstitution", form.studyInstitution(), errors);
      notAllowed("studyProgramme", form.studyProgramme(), errors);
      notAllowed("studentId", form.studentId(), errors);
    }
    List<ConferenceOption> options = options(form.optionIds(), type, errors);
    List<Consent> consents = consents(form.consentIds(), errors);
    if (isBlank(form.captchaToken())) {
      errors.add(new FieldError("captchaToken", FieldError.REQUIRED));
    }
    if (!errors.isEmpty() || type.isEmpty()) {
      throw new ValidationException(errors);
    }
    return new ValidRegistration(
        type.get(),
        new AcceptedRegistration.Participant(
            firstName, lastName, email, organization, null, null, null),
        options,
        consents);
  }

  private static Optional<RegistrationType> type(String raw, List<FieldError> errors) {
    if (isBlank(raw)) {
      errors.add(new FieldError("type", FieldError.REQUIRED));
      return Optional.empty();
    }
    for (RegistrationType type : RegistrationType.values()) {
      if (type.name().equals(raw.strip())) {
        return Optional.of(type);
      }
    }
    errors.add(new FieldError("type", FieldError.INVALID_VALUE));
    return Optional.empty();
  }

  /** Trimmed required text, or null with an error. */
  private static String text(String field, String raw, List<FieldError> errors) {
    return checked(field, raw, MAX_TEXT, errors);
  }

  private static String email(String raw, List<FieldError> errors) {
    String email = checked("email", raw, MAX_EMAIL, errors);
    if (email != null && !validEmail(email)) {
      errors.add(new FieldError("email", FieldError.INVALID_EMAIL));
      return null;
    }
    return email;
  }

  private static String checked(String field, String raw, int max, List<FieldError> errors) {
    String value = raw == null ? "" : raw.strip();
    if (value.isEmpty()) {
      errors.add(new FieldError(field, FieldError.REQUIRED));
      return null;
    }
    if (value.codePointCount(0, value.length()) > max) {
      errors.add(new FieldError(field, FieldError.TOO_LONG));
      return null;
    }
    if (value.codePoints().anyMatch(c -> Character.getType(c) == Character.CONTROL)) {
      errors.add(new FieldError(field, FieldError.INVALID_CHARACTERS));
      return null;
    }
    return value;
  }

  static boolean validEmail(String email) {
    int at = email.indexOf('@');
    if (at <= 0 || at != email.lastIndexOf('@') || at == email.length() - 1) {
      return false;
    }
    String local = email.substring(0, at);
    String domain = email.substring(at + 1);
    return local.length() <= MAX_LOCAL_PART
        && LOCAL_PART.matcher(local).matches()
        && DOMAIN.matcher(domain).matches();
  }

  private static void notAllowed(String field, String raw, List<FieldError> errors) {
    if (!isBlank(raw)) {
      errors.add(new FieldError(field, FieldError.NOT_ALLOWED));
    }
  }

  private List<ConferenceOption> options(
      List<String> ids, Optional<RegistrationType> type, List<FieldError> errors) {
    Set<String> codes = new LinkedHashSet<>();
    Set<String> seen = new HashSet<>();
    List<ConferenceOption> selected = new ArrayList<>();
    for (String id : ids) {
      Optional<ConferenceOption> option = id == null ? Optional.empty() : catalog.option(id);
      if (option.isEmpty()) {
        codes.add(FieldError.UNKNOWN_OPTION);
      } else if (!seen.add(id)) {
        codes.add(FieldError.DUPLICATE_OPTION);
      } else if (!option.get().active()
          || type.isPresent() && !option.get().selectableBy(type.get())) {
        codes.add(FieldError.OPTION_NOT_AVAILABLE);
      } else {
        selected.add(option.get());
      }
    }
    codes.forEach(code -> errors.add(new FieldError("optionIds", code)));
    return selected;
  }

  private List<Consent> consents(List<String> ids, List<FieldError> errors) {
    Set<String> given = new LinkedHashSet<>();
    for (String id : ids) {
      if (id == null || catalog.consent(id).isEmpty()) {
        errors.add(new FieldError("consentIds", FieldError.UNKNOWN_CONSENT));
      } else {
        given.add(id);
      }
    }
    List<Consent> consents = new ArrayList<>();
    for (Consent consent : catalog.consents()) {
      if (given.contains(consent.id())) {
        consents.add(consent);
      } else if (consent.mandatory()) {
        errors.add(new FieldError("consents." + consent.id(), FieldError.CONSENT_REQUIRED));
      }
    }
    return consents;
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
