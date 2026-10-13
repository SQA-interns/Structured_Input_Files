package si.konferenca.registration.domain;

import java.util.List;

/** A registration as submitted, before validation (OpenAPI RegistrationRequest). */
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
    List<String> consents) {

  public RegistrationSubmission {
    optionIds =
        optionIds == null
            ? null
            : java.util.Collections.unmodifiableList(new java.util.ArrayList<>(optionIds));
    consents =
        consents == null
            ? null
            : java.util.Collections.unmodifiableList(new java.util.ArrayList<>(consents));
  }
}
