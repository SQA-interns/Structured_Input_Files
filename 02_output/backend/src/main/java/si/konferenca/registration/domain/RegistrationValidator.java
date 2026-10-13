package si.konferenca.registration.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/** Field rules of a registration (spec 4.2; BR-01..05, SR-04, SB-01). */
public final class RegistrationValidator {

  public static final String REQUIRED = "This field is required.";
  public static final String INVALID_EMAIL = "Enter a valid email address.";
  public static final String CHOOSE_TYPE = "Choose a registration type.";
  public static final String NOT_APPLICABLE = "This field does not apply to the registration type.";
  public static final String INVALID_OPTIONS = "Select only available options, each at most once.";
  public static final String CONSENT_REQUIRED = "This consent is required.";
  public static final String UNKNOWN_CONSENT = "Unknown consent.";

  static final int NAME_MAX = 100;
  static final int EMAIL_MAX = 254;
  static final int TEXT_MAX = 200;
  static final int STUDENT_ID_MAX = 50;
  static final int OPTIONS_MAX = 100;

  private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

  private final ConferenceCatalog catalog;

  public RegistrationValidator(ConferenceCatalog catalog) {
    this.catalog = catalog;
  }

  /** The validated registration, or the field errors. */
  public Result validate(RegistrationSubmission s) {
    FieldErrors errors = new FieldErrors();
    Optional<RegistrationType> type = type(s.type());
    if (type.isEmpty()) {
      errors.add("type", CHOOSE_TYPE);
    }
    String firstName = required(errors, "firstName", s.firstName(), NAME_MAX);
    String lastName = required(errors, "lastName", s.lastName(), NAME_MAX);
    String email = email(errors, s.email());
    boolean external = type.orElse(null) == RegistrationType.EXTERNAL;
    boolean student = type.orElse(null) == RegistrationType.STUDENT;
    String organization =
        field(errors, external, student, "organization", s.organization(), TEXT_MAX);
    String institution =
        field(errors, student, external, "studyInstitution", s.studyInstitution(), TEXT_MAX);
    String programme =
        field(errors, student, external, "studyProgramme", s.studyProgramme(), TEXT_MAX);
    String studentId = field(errors, student, external, "studentId", s.studentId(), STUDENT_ID_MAX);
    List<ConferenceOption> options = options(errors, s.optionIds(), type.orElse(null));
    List<ConsentDefinition> consents = consents(errors, s.consents());
    if (!errors.isEmpty()) {
      return new Result(null, errors.asMap());
    }
    return new Result(
        new ValidatedRegistration(
            type.orElseThrow(),
            firstName,
            lastName,
            email,
            organization,
            institution,
            programme,
            studentId,
            options,
            consents),
        Map.of());
  }

  private static Optional<RegistrationType> type(String value) {
    for (RegistrationType candidate : RegistrationType.values()) {
      if (candidate.name().equals(value)) {
        return Optional.of(candidate);
      }
    }
    return Optional.empty();
  }

  private static String trimmed(String value) {
    return value == null ? "" : value.strip();
  }

  private static String required(FieldErrors errors, String field, String value, int max) {
    String text = trimmed(value);
    if (text.isEmpty()) {
      errors.add(field, REQUIRED);
    } else if (text.length() > max) {
      errors.add(field, "Use at most " + max + " characters.");
    }
    return text;
  }

  /** Required when the field belongs to the type, empty when it belongs to the other type. */
  private static String field(
      FieldErrors errors, boolean own, boolean other, String field, String value, int max) {
    if (own) {
      return required(errors, field, value, max);
    }
    if (other && !trimmed(value).isEmpty()) {
      errors.add(field, NOT_APPLICABLE);
    }
    return null;
  }

  private static String email(FieldErrors errors, String value) {
    String text = trimmed(value);
    if (text.isEmpty()) {
      errors.add("email", REQUIRED);
    } else if (text.length() > EMAIL_MAX || !EMAIL.matcher(text).matches()) {
      errors.add("email", INVALID_EMAIL);
    }
    return text;
  }

  private List<ConferenceOption> options(
      FieldErrors errors, List<String> ids, RegistrationType type) {
    List<ConferenceOption> selected = new ArrayList<>();
    if (ids == null) {
      return selected;
    }
    Set<String> seen = new HashSet<>();
    boolean valid = ids.size() <= OPTIONS_MAX;
    for (String id : ids) {
      Optional<ConferenceOption> option = id == null ? Optional.empty() : catalog.option(id);
      boolean available =
          option.isPresent()
              && (type == null ? option.get().active() : option.get().selectableBy(type));
      if (!available || !seen.add(id)) {
        valid = false;
      } else {
        selected.add(option.get());
      }
    }
    if (!valid) {
      errors.add("optionIds", INVALID_OPTIONS);
    }
    return selected;
  }

  private List<ConsentDefinition> consents(FieldErrors errors, List<String> given) {
    Set<String> ids = given == null ? Set.of() : new HashSet<>(given);
    for (String id : ids) {
      if (id == null || catalog.consent(id).isEmpty()) {
        errors.add("consents", UNKNOWN_CONSENT);
      }
    }
    List<ConsentDefinition> consents = new ArrayList<>();
    for (ConsentDefinition consent : catalog.consents()) {
      if (ids.contains(consent.id())) {
        consents.add(consent);
      } else if (consent.mandatory()) {
        errors.add("consents." + consent.id(), CONSENT_REQUIRED);
      }
    }
    return consents;
  }

  /** Either a validated registration (no errors) or field errors (registration null). */
  public record Result(ValidatedRegistration registration, Map<String, String> errors) {

    public Result {
      errors = Map.copyOf(errors);
    }

    public boolean valid() {
      return registration != null;
    }
  }
}
