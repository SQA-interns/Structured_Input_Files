package si.konferenca.registration.domain;

import java.util.List;

/** A registration as submitted, before validation (openapi.json RegistrationRequest). */
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
    String captchaToken,
    String clientAddress) {}
