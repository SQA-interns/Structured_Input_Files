package si.konferenca.registration.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.RegistrationType;

/** Table {@code registration} (contract database-schema.sql). */
@Entity
@Table(name = "registration")
class RegistrationEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "reference", nullable = false, updatable = false)
  private UUID reference;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private RegistrationType type;

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

  @Column(name = "submitted_at", nullable = false)
  private Instant submittedAt;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "registration_option",
      joinColumns = @JoinColumn(name = "registration_id"))
  @OrderColumn(name = "position")
  private List<OptionRow> options = new ArrayList<>();

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "registration_consent",
      joinColumns = @JoinColumn(name = "registration_id"))
  private List<ConsentRow> consents = new ArrayList<>();

  protected RegistrationEntity() {}

  RegistrationEntity(
      UUID reference,
      RegistrationType type,
      String firstName,
      String lastName,
      String email,
      String emailNormalized,
      String organization,
      String studyInstitution,
      String studyProgramme,
      String studentId,
      Instant submittedAt) {
    this.reference = reference;
    this.type = type;
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.emailNormalized = emailNormalized;
    this.organization = organization;
    this.studyInstitution = studyInstitution;
    this.studyProgramme = studyProgramme;
    this.studentId = studentId;
    this.submittedAt = submittedAt;
  }

  UUID reference() {
    return reference;
  }

  RegistrationType type() {
    return type;
  }

  String firstName() {
    return firstName;
  }

  String lastName() {
    return lastName;
  }

  String email() {
    return email;
  }

  String organization() {
    return organization;
  }

  String studyInstitution() {
    return studyInstitution;
  }

  String studyProgramme() {
    return studyProgramme;
  }

  String studentId() {
    return studentId;
  }

  Instant submittedAt() {
    return submittedAt;
  }

  List<OptionRow> options() {
    return options;
  }

  List<ConsentRow> consents() {
    return consents;
  }

  /** Row of {@code registration_option}. */
  @Embeddable
  static class OptionRow {
    @Column(name = "option_id", nullable = false)
    private String optionId;

    @Column(name = "option_name", nullable = false)
    private String optionName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private OptionCategory category;

    protected OptionRow() {}

    OptionRow(String optionId, String optionName, OptionCategory category) {
      this.optionId = optionId;
      this.optionName = optionName;
      this.category = category;
    }

    String optionId() {
      return optionId;
    }

    String optionName() {
      return optionName;
    }

    OptionCategory category() {
      return category;
    }
  }

  /** Row of {@code registration_consent}. */
  @Embeddable
  static class ConsentRow {
    @Column(name = "consent_id", nullable = false)
    private String consentId;

    @Column(name = "consent_text", nullable = false)
    private String consentText;

    @Column(name = "given_at", nullable = false)
    private Instant givenAt;

    protected ConsentRow() {}

    ConsentRow(String consentId, String consentText, Instant givenAt) {
      this.consentId = consentId;
      this.consentText = consentText;
      this.givenAt = givenAt;
    }

    String consentId() {
      return consentId;
    }

    String consentText() {
      return consentText;
    }

    Instant givenAt() {
      return givenAt;
    }
  }
}
