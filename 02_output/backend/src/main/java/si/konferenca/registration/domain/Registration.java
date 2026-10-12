package si.konferenca.registration.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** An accepted registration (BR-07, docs/02_contracts/database.sql). */
@Entity
@Table(name = "registration")
public class Registration {

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

  @Column(name = "student_id", length = 50)
  private String studentId;

  @Column(name = "registered_at", nullable = false)
  private Instant registeredAt;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "registration_option",
      joinColumns = @JoinColumn(name = "registration_id"))
  @OrderColumn(name = "position")
  private List<SelectedOption> options = new ArrayList<>();

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "registration_consent",
      joinColumns = @JoinColumn(name = "registration_id"))
  private List<GivenConsent> consents = new ArrayList<>();

  protected Registration() {}

  /** A new registration of a participant; the fields not used by the type stay null. */
  public Registration(
      UUID id, RegistrationType type, ParticipantDetails participant, Instant registeredAt) {
    this.id = id;
    this.type = type;
    this.firstName = participant.firstName();
    this.lastName = participant.lastName();
    this.email = participant.email();
    this.organization = participant.organization();
    this.studyInstitution = participant.studyInstitution();
    this.studyProgramme = participant.studyProgramme();
    this.studentId = participant.studentId();
    this.registeredAt = registeredAt;
  }

  public void addOption(SelectedOption option) {
    options.add(option);
  }

  public void addConsent(GivenConsent consent) {
    consents.add(consent);
  }

  public UUID getId() {
    return id;
  }

  public RegistrationType getType() {
    return type;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getEmail() {
    return email;
  }

  public String getOrganization() {
    return organization;
  }

  public String getStudyInstitution() {
    return studyInstitution;
  }

  public String getStudyProgramme() {
    return studyProgramme;
  }

  public String getStudentId() {
    return studentId;
  }

  public Instant getRegisteredAt() {
    return registeredAt;
  }

  public List<SelectedOption> getOptions() {
    return List.copyOf(options);
  }

  public List<GivenConsent> getConsents() {
    return List.copyOf(consents);
  }
}
