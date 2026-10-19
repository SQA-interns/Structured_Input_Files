package si.konferenca.registration.domain;

import java.util.List;

/** A submission as received: untrusted, untrimmed, possibly incomplete (SB-01). */
public record RegistrationSubmission(
    String type,
    String firstName,
    String lastName,
    String email,
    String organization,
    String studyInstitution,
    String studyProgramme,
    String studentId,
    List<String> optionIds,
    List<String> consentIds,
    String recaptchaToken) {}
