package si.konferenca.registration.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A registration as submitted, before validation. Values may be null; {@link #normalized()} trims
 * text and replaces missing lists and maps (BR-02).
 */
public record Submission(
    String type,
    String firstName,
    String lastName,
    String email,
    String organization,
    String studyInstitution,
    String studyProgramme,
    String studentId,
    List<String> optionIds,
    Map<String, Boolean> consents,
    String recaptchaToken) {

  /** A copy with every text value stripped and null collections replaced by empty ones. */
  public Submission normalized() {
    return new Submission(
        strip(type),
        strip(firstName),
        strip(lastName),
        strip(email),
        strip(organization),
        strip(studyInstitution),
        strip(studyProgramme),
        strip(studentId),
        optionIds == null ? List.of() : new ArrayList<>(optionIds),
        consents == null ? Map.of() : new LinkedHashMap<>(consents),
        strip(recaptchaToken));
  }

  private static String strip(String value) {
    return value == null ? null : value.strip();
  }
}
