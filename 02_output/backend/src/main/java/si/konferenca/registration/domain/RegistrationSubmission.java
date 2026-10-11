package si.konferenca.registration.domain;

import java.util.ArrayList;
import java.util.Collections;
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
    String clientAddress) {

  public RegistrationSubmission {
    optionIds = optionIds == null ? null : Collections.unmodifiableList(new ArrayList<>(optionIds));
    consentIds =
        consentIds == null ? null : Collections.unmodifiableList(new ArrayList<>(consentIds));
  }
}
