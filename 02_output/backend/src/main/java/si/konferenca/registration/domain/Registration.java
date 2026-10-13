package si.konferenca.registration.domain;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** An accepted registration (BR-07): the stored, copied, mailed and exported record. */
public record Registration(
    UUID id,
    RegistrationType type,
    Instant registeredAt,
    String firstName,
    String lastName,
    String email,
    String organization,
    String studyInstitution,
    String studyProgramme,
    String studentId,
    List<SelectedOption> options,
    List<GivenConsent> consents) {

  private static final DateTimeFormatter ISO_MILLIS =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

  public Registration {
    options = List.copyOf(options);
    consents = List.copyOf(consents);
  }

  /** An option as selected, with its name and category at the time of registration. */
  public record SelectedOption(String id, String name, OptionCategory category) {}

  /** A consent as given, with the wording shown and the time it was given (SB-14). */
  public record GivenConsent(String id, String text, Instant givenAt) {}

  /**
   * Builds the accepted registration from a valid, normalised submission. Fields of the other type
   * are dropped, consents not given are not recorded.
   */
  public static Registration accept(
      Submission valid, ConferenceCatalogue catalogue, UUID id, Instant registeredAt) {
    RegistrationType type = RegistrationType.parse(valid.type()).orElseThrow();
    boolean external = type == RegistrationType.EXTERNAL;
    List<SelectedOption> options = new ArrayList<>();
    for (String optionId : valid.optionIds()) {
      ConferenceOption option = catalogue.option(optionId).orElseThrow();
      options.add(new SelectedOption(option.id(), option.name(), option.category()));
    }
    List<GivenConsent> consents = new ArrayList<>();
    for (ConsentDefinition consent : catalogue.consents()) {
      if (Boolean.TRUE.equals(valid.consents().get(consent.id()))) {
        consents.add(new GivenConsent(consent.id(), consent.text(), registeredAt));
      }
    }
    return new Registration(
        id,
        type,
        registeredAt,
        valid.firstName(),
        valid.lastName(),
        valid.email(),
        external ? valid.organization() : null,
        external ? null : valid.studyInstitution(),
        external ? null : valid.studyProgramme(),
        external ? null : valid.studentId(),
        options,
        consents);
  }

  /** ISO-8601 UTC with milliseconds, as used in the JSON copy, emails and the export. */
  public static String format(Instant instant) {
    return ISO_MILLIS.format(instant);
  }
}
