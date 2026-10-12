package si.konferenca.registration.service;

import java.util.List;

/** A submitted registration as received, before validation (specification 4.2). */
public record RegistrationCommand(
    String type,
    String firstName,
    String lastName,
    String email,
    String organization,
    String studyInstitution,
    String studyProgramme,
    String studentId,
    List<String> optionIds,
    List<String> consents,
    String recaptchaToken,
    String clientIp) {}
