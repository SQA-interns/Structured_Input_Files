package si.konferenca.registration.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A submitted registration exactly as received, before validation. */
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
    Boolean consentGiven) {

  public RegistrationForm {
    optionIds = optionIds == null ? null : Collections.unmodifiableList(new ArrayList<>(optionIds));
  }
}
