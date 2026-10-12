package si.konferenca.registration.infrastructure.jsoncopy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.format.DateTimeFormatter;
import java.util.List;
import si.konferenca.registration.domain.Registration;

/** Shape of the JSON copy (contract {@code registration-copy.schema.json}). */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
  "id",
  "type",
  "submittedAt",
  "firstName",
  "lastName",
  "email",
  "organization",
  "studyInstitution",
  "studyProgramme",
  "studentId",
  "selectedOptions",
  "consent"
})
record RegistrationCopy(
    String id,
    String type,
    String submittedAt,
    String firstName,
    String lastName,
    String email,
    String organization,
    String studyInstitution,
    String studyProgramme,
    String studentId,
    List<Option> selectedOptions,
    Consent consent) {

  @JsonPropertyOrder({"id", "name", "category"})
  record Option(String id, String name, String category) {}

  @JsonPropertyOrder({"id", "text", "givenAt"})
  record Consent(String id, String text, String givenAt) {}

  static RegistrationCopy of(Registration r) {
    return new RegistrationCopy(
        r.id().toString(),
        r.type().name(),
        DateTimeFormatter.ISO_INSTANT.format(r.submittedAt()),
        r.firstName(),
        r.lastName(),
        r.email(),
        r.organization(),
        r.studyInstitution(),
        r.studyProgramme(),
        r.studentId(),
        r.selectedOptions().stream()
            .map(o -> new Option(o.id(), o.name(), o.category().name()))
            .toList(),
        new Consent(
            r.consent().id(),
            r.consent().text(),
            DateTimeFormatter.ISO_INSTANT.format(r.consent().givenAt())));
  }
}
