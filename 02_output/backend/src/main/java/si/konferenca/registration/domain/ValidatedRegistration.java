package si.konferenca.registration.domain;

import java.util.List;

/**
 * A submission that passed every rule: trimmed fields of its type only, selected options in
 * configuration order, given consent ids.
 */
public record ValidatedRegistration(
    RegistrationType type,
    String firstName,
    String lastName,
    String email,
    String organization,
    String studyInstitution,
    String studyProgramme,
    String studentId,
    List<ConferenceOption> options,
    List<String> consentIds,
    String recaptchaToken) {}
