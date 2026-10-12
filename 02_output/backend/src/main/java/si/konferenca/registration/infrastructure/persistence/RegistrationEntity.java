package si.konferenca.registration.infrastructure.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import si.konferenca.registration.domain.Category;
import si.konferenca.registration.domain.ConsentRecord;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;
import si.konferenca.registration.domain.SelectedOption;

/** Table {@code registration} (contract {@code database.md}). */
@Entity
@Table(name = "registration")
class RegistrationEntity {

  @Id private UUID id;

  @Column(name = "type", nullable = false)
  private String type;

  @Column(name = "submitted_at", nullable = false)
  private Instant submittedAt;

  @Column(name = "first_name", nullable = false)
  private String firstName;

  @Column(name = "last_name", nullable = false)
  private String lastName;

  @Column(name = "email", nullable = false)
  private String email;

  @Column(name = "email_normalized", nullable = false)
  private String emailNormalized;

  @Column(name = "organization")
  private String organization;

  @Column(name = "study_institution")
  private String studyInstitution;

  @Column(name = "study_programme")
  private String studyProgramme;

  @Column(name = "student_id")
  private String studentId;

  @Column(name = "consent_id", nullable = false)
  private String consentId;

  @Column(name = "consent_text", nullable = false)
  private String consentText;

  @Column(name = "consent_given_at", nullable = false)
  private Instant consentGivenAt;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "registration_option",
      joinColumns = @JoinColumn(name = "registration_id"))
  @OrderColumn(name = "position")
  private List<RegistrationOptionEmbeddable> options = new ArrayList<>();

  protected RegistrationEntity() {}

  static RegistrationEntity from(Registration r) {
    RegistrationEntity e = new RegistrationEntity();
    e.id = r.id();
    e.type = r.type().name();
    e.submittedAt = r.submittedAt();
    e.firstName = r.firstName();
    e.lastName = r.lastName();
    e.email = r.email();
    e.emailNormalized = r.normalizedEmail();
    e.organization = r.organization();
    e.studyInstitution = r.studyInstitution();
    e.studyProgramme = r.studyProgramme();
    e.studentId = r.studentId();
    e.consentId = r.consent().id();
    e.consentText = r.consent().text();
    e.consentGivenAt = r.consent().givenAt();
    for (SelectedOption o : r.selectedOptions()) {
      e.options.add(new RegistrationOptionEmbeddable(o.id(), o.name(), o.category().name()));
    }
    return e;
  }

  Registration toDomain() {
    List<SelectedOption> selected =
        options.stream()
            .map(
                o ->
                    new SelectedOption(
                        o.optionId(), o.optionName(), Category.valueOf(o.category())))
            .toList();
    return new Registration(
        id,
        RegistrationType.valueOf(type),
        submittedAt,
        firstName,
        lastName,
        email,
        organization,
        studyInstitution,
        studyProgramme,
        studentId,
        selected,
        new ConsentRecord(consentId, consentText, consentGivenAt));
  }
}
