package si.konferenca.registration.infrastructure.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;

/** Table registration with its option and consent rows (database.sql). */
@Entity
@Table(name = "registration")
public class RegistrationEntity {

  @Id private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private RegistrationType type;

  @Column(name = "first_name", nullable = false, length = 100)
  private String firstName;

  @Column(name = "last_name", nullable = false, length = 100)
  private String lastName;

  @Column(nullable = false, length = 254)
  private String email;

  @Column(length = 200)
  private String organization;

  @Column(name = "study_institution", length = 200)
  private String studyInstitution;

  @Column(name = "study_programme", length = 200)
  private String studyProgramme;

  @Column(name = "student_id", length = 200)
  private String studentId;

  @Column(name = "registered_at", nullable = false)
  private Instant registeredAt;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "registration_option",
      joinColumns = @JoinColumn(name = "registration_id"))
  @Column(name = "option_id", nullable = false, length = 50)
  private Set<String> optionIds = new LinkedHashSet<>();

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "registration_consent",
      joinColumns = @JoinColumn(name = "registration_id"))
  private Set<ConsentEmbeddable> consents = new LinkedHashSet<>();

  protected RegistrationEntity() {}

  static RegistrationEntity from(Registration registration) {
    RegistrationEntity entity = new RegistrationEntity();
    entity.id = registration.id();
    entity.type = registration.type();
    entity.firstName = registration.firstName();
    entity.lastName = registration.lastName();
    entity.email = registration.email();
    entity.organization = registration.organization();
    entity.studyInstitution = registration.studyInstitution();
    entity.studyProgramme = registration.studyProgramme();
    entity.studentId = registration.studentId();
    entity.registeredAt = registration.registeredAt();
    registration.options().forEach(o -> entity.optionIds.add(o.id()));
    registration
        .consents()
        .forEach(c -> entity.consents.add(new ConsentEmbeddable(c.id(), c.givenAt())));
    return entity;
  }
}
