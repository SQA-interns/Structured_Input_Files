package si.konferenca.registration.service;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import si.konferenca.registration.conference.ConferenceConfiguration;
import si.konferenca.registration.conference.ConferenceOption;
import si.konferenca.registration.conference.ConsentDefinition;
import si.konferenca.registration.domain.ParticipantDetails;
import si.konferenca.registration.domain.RegistrationType;

/** Server-side field rules of specification 4.2 step 2 (SB-01, BR-01 … BR-05). */
@Component
public class RegistrationValidator {

  static final String REQUIRED = "This field is required.";
  static final String TOO_LONG = "This field is too long.";
  static final String INVALID_EMAIL = "Enter a valid email address.";
  static final String NOT_FOR_TYPE = "This field does not belong to the chosen registration type.";
  static final String INVALID_TYPE = "Choose external participant or student.";
  static final String INVALID_OPTIONS = "One or more selected options are not available.";
  static final String UNKNOWN_CONSENT = "One or more consents are unknown.";
  static final String CONSENT_REQUIRED = "This consent is required.";
  static final String TOKEN_REQUIRED = "Confirm that you are not a robot.";

  private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

  /** Field name to maximum length; the order is the order of the error list. */
  private static final Map<String, Integer> MAX_LENGTH = new LinkedHashMap<>();

  static {
    MAX_LENGTH.put("firstName", 100);
    MAX_LENGTH.put("lastName", 100);
    MAX_LENGTH.put("email", 254);
    MAX_LENGTH.put("organization", 200);
    MAX_LENGTH.put("studyInstitution", 200);
    MAX_LENGTH.put("studyProgramme", 200);
    MAX_LENGTH.put("studentId", 50);
  }

  private static final Map<RegistrationType, List<String>> FIELDS =
      Map.of(
          RegistrationType.EXTERNAL,
          List.of("firstName", "lastName", "email", "organization"),
          RegistrationType.STUDENT,
          List.of(
              "firstName", "lastName", "email", "studyInstitution", "studyProgramme", "studentId"));

  private final ConferenceConfiguration conference;

  public RegistrationValidator(ConferenceConfiguration conference) {
    this.conference = conference;
  }

  /** Returns the validated registration or throws with every field error found. */
  public ValidatedRegistration validate(RegistrationCommand command) {
    List<FieldError> errors = new ArrayList<>();
    Optional<RegistrationType> type = type(command.type());
    if (type.isEmpty()) {
      errors.add(new FieldError("type", INVALID_TYPE));
    }
    Map<String, String> values = trimmedValues(command);
    type.ifPresent(t -> checkFields(t, values, errors));
    List<ConferenceOption> options = type.map(t -> options(t, command, errors)).orElse(List.of());
    List<ConsentDefinition> consents = consents(command, errors);
    if (isBlank(command.recaptchaToken())) {
      errors.add(new FieldError("recaptchaToken", TOKEN_REQUIRED));
    }
    if (!errors.isEmpty()) {
      throw new RegistrationRejectedException(errors);
    }
    RegistrationType registrationType = type.orElseThrow();
    List<String> used = FIELDS.get(registrationType);
    return new ValidatedRegistration(
        registrationType,
        new ParticipantDetails(
            values.get("firstName"),
            values.get("lastName"),
            values.get("email"),
            used.contains("organization") ? values.get("organization") : null,
            used.contains("studyInstitution") ? values.get("studyInstitution") : null,
            used.contains("studyProgramme") ? values.get("studyProgramme") : null,
            used.contains("studentId") ? values.get("studentId") : null),
        options,
        consents);
  }

  private static Optional<RegistrationType> type(String value) {
    if (value == null) {
      return Optional.empty();
    }
    for (RegistrationType type : RegistrationType.values()) {
      if (type.name().equals(value.trim())) {
        return Optional.of(type);
      }
    }
    return Optional.empty();
  }

  private static Map<String, String> trimmedValues(RegistrationCommand command) {
    Map<String, String> values = new LinkedHashMap<>();
    values.put("firstName", trim(command.firstName()));
    values.put("lastName", trim(command.lastName()));
    values.put("email", trim(command.email()));
    values.put("organization", trim(command.organization()));
    values.put("studyInstitution", trim(command.studyInstitution()));
    values.put("studyProgramme", trim(command.studyProgramme()));
    values.put("studentId", trim(command.studentId()));
    return values;
  }

  private static void checkFields(
      RegistrationType type, Map<String, String> values, List<FieldError> errors) {
    List<String> used = FIELDS.get(type);
    for (Map.Entry<String, Integer> field : MAX_LENGTH.entrySet()) {
      String name = field.getKey();
      String value = values.get(name);
      if (!used.contains(name)) {
        if (!isBlank(value)) {
          errors.add(new FieldError(name, NOT_FOR_TYPE));
        }
      } else if (isBlank(value)) {
        errors.add(new FieldError(name, REQUIRED));
      } else if (value.length() > field.getValue()) {
        errors.add(new FieldError(name, TOO_LONG));
      } else if ("email".equals(name) && !isValidEmail(value)) {
        errors.add(new FieldError(name, INVALID_EMAIL));
      }
    }
  }

  static boolean isValidEmail(String email) {
    if (!EMAIL.matcher(email).matches()) {
      return false;
    }
    try {
      new InternetAddress(email, true).validate();
      return true;
    } catch (AddressException e) {
      return false;
    }
  }

  private List<ConferenceOption> options(
      RegistrationType type, RegistrationCommand command, List<FieldError> errors) {
    List<String> ids = command.optionIds() == null ? List.of() : command.optionIds();
    List<ConferenceOption> options = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    boolean valid = true;
    for (String id : ids) {
      Optional<ConferenceOption> option = id == null ? Optional.empty() : conference.option(id);
      if (option.isEmpty() || !option.get().selectableBy(type) || !seen.add(id)) {
        valid = false;
      } else {
        options.add(option.get());
      }
    }
    if (!valid) {
      errors.add(new FieldError("optionIds", INVALID_OPTIONS));
    }
    return options;
  }

  private List<ConsentDefinition> consents(RegistrationCommand command, List<FieldError> errors) {
    Set<String> given = new HashSet<>(command.consents() == null ? List.of() : command.consents());
    List<ConsentDefinition> consents = new ArrayList<>();
    for (ConsentDefinition consent : conference.consents()) {
      if (given.remove(consent.id())) {
        consents.add(consent);
      } else if (consent.mandatory()) {
        errors.add(new FieldError("consents." + consent.id(), CONSENT_REQUIRED));
      }
    }
    if (!given.isEmpty()) {
      errors.add(new FieldError("consents", UNKNOWN_CONSENT));
    }
    return consents;
  }

  private static String trim(String value) {
    return value == null ? null : value.strip();
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
