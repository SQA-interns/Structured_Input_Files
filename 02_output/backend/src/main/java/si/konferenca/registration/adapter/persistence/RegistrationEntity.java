package si.konferenca.registration.adapter.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Participant;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;
import si.konferenca.registration.domain.SelectedOption;

/** Table registration (contract database.md). */
@Entity
@Table(name = "registration")
public class RegistrationEntity {

  @Id private UUID id;

  @Column(name = "type", nullable = false, length = 16)
  private String type;

  @Column(name = "first_name", nullable = false, length = 100)
  private String firstName;

  @Column(name = "last_name", nullable = false, length = 100)
  private String lastName;

  @Column(name = "email", nullable = false, length = 254)
  private String email;

  @Column(name = "organization", length = 200)
  private String organization;

  @Column(name = "study_institution", length = 200)
  private String studyInstitution;

  @Column(name = "study_programme", length = 200)
  private String studyProgramme;

  @Column(name = "student_id", length = 50)
  private String studentId;

  @Column(name = "registered_at", nullable = false)
  private Instant registeredAt;

  @ElementCollection
  @CollectionTable(
      name = "registration_option",
      joinColumns = @JoinColumn(name = "registration_id"))
  private Set<OptionRow> options = new LinkedHashSet<>();

  @ElementCollection
  @CollectionTable(
      name = "registration_consent",
      joinColumns = @JoinColumn(name = "registration_id"))
  private Set<ConsentRow> consents = new LinkedHashSet<>();

  protected RegistrationEntity() {}

  RegistrationEntity(Registration r) {
    Participant p = r.participant();
    this.id = r.id();
    this.type = r.type().name();
    this.firstName = p.firstName();
    this.lastName = p.lastName();
    this.email = p.email();
    this.organization = p.organization();
    this.studyInstitution = p.studyInstitution();
    this.studyProgramme = p.studyProgramme();
    this.studentId = p.studentId();
    this.registeredAt = r.registeredAt();
    r.options().forEach(o -> options.add(new OptionRow(o.id(), o.name(), o.category().name())));
    r.consents().forEach(c -> consents.add(new ConsentRow(c.id(), c.text(), c.givenAt())));
  }

  Registration toDomain() {
    return new Registration(
        id,
        RegistrationType.valueOf(type),
        registeredAt,
        new Participant(
            firstName, lastName, email, organization, studyInstitution, studyProgramme, studentId),
        options.stream()
            .sorted(Comparator.comparing(OptionRow::optionId))
            .map(
                o ->
                    new SelectedOption(
                        o.optionId(), o.optionName(), OptionCategory.valueOf(o.category())))
            .toList(),
        consents.stream()
            .sorted(Comparator.comparing(ConsentRow::consentId))
            .map(c -> new GivenConsent(c.consentId(), c.consentText(), c.givenAt()))
            .toList());
  }
}
