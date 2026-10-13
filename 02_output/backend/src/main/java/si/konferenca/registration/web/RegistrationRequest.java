package si.konferenca.registration.web;

import java.util.List;
import si.konferenca.registration.domain.RegistrationSubmission;

/** OpenAPI schema RegistrationRequest; unknown properties are rejected. */
public record RegistrationRequest(
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
    String recaptchaToken) {

  RegistrationSubmission toSubmission() {
    return new RegistrationSubmission(
        type,
        firstName,
        lastName,
        email,
        organization,
        studyInstitution,
        studyProgramme,
        studentId,
        optionIds,
        consents);
  }
}
