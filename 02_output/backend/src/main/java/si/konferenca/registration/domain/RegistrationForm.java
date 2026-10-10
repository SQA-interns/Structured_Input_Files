package si.konferenca.registration.domain;

import java.util.List;

/** A submitted registration as received, before validation; any value may be missing. */
public record RegistrationForm(
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
    String captchaToken) {

  public RegistrationForm {
    optionIds = optionIds == null ? List.of() : optionIds.stream().toList();
    consentIds = consentIds == null ? List.of() : consentIds.stream().toList();
  }
}
