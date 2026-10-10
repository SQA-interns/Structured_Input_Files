package si.konferenca.registration.domain;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** A registration exactly as accepted: trimmed values, option and consent snapshots (BR-07). */
public record AcceptedRegistration(
    UUID reference,
    Instant submittedAt,
    RegistrationType type,
    Participant participant,
    List<SelectedOption> options,
    List<GivenConsent> consents) {

  public AcceptedRegistration {
    options = List.copyOf(options);
    consents = List.copyOf(consents);
  }

  /** The email as compared for duplicates (D-12). */
  public String normalizedEmail() {
    return normalizeEmail(participant.email());
  }

  public static String normalizeEmail(String email) {
    return email.strip().toLowerCase(Locale.ROOT);
  }

  /** Participant fields; the fields of the other type are null (BR-01). */
  public record Participant(
      String firstName,
      String lastName,
      String email,
      String organization,
      String studyInstitution,
      String studyProgramme,
      String studentId) {}

  /** Snapshot of a selected option. */
  public record SelectedOption(String id, String name, OptionCategory category) {}

  /** Snapshot of a given consent with the time it was given. */
  public record GivenConsent(String id, String text, Instant givenAt) {}
}
