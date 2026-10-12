package si.konferenca.registration.domain;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** An accepted registration with trimmed values (BR-01, BR-02, BR-07). */
public record Registration(
    UUID id,
    RegistrationType type,
    Instant submittedAt,
    String firstName,
    String lastName,
    String email,
    String organization,
    String studyInstitution,
    String studyProgramme,
    String studentId,
    List<SelectedOption> selectedOptions,
    ConsentRecord consent) {

  public Registration {
    selectedOptions = List.copyOf(selectedOptions);
  }

  /** The email as compared for duplicates (D-14). */
  public String normalizedEmail() {
    return normalizeEmail(email);
  }

  public static String normalizeEmail(String email) {
    return email.strip().toLowerCase(Locale.ROOT);
  }
}
