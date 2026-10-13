package si.konferenca.registration.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.BatchSize;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.RegistrationType;

/** Table {@code registration} with its options and consents (contract database.sql). */
@Entity
@Table(name = "registration")
public class RegistrationEntity {

  @Id private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(name = "registration_type", nullable = false)
  private RegistrationType type;

  @Column(name = "first_name", nullable = false)
  private String firstName;

  @Column(name = "last_name", nullable = false)
  private String lastName;

  @Column(nullable = false)
  private String email;

  private String organization;

  @Column(name = "study_institution")
  private String studyInstitution;

  @Column(name = "study_programme")
  private String studyProgramme;

  @Column(name = "student_id")
  private String studentId;

  @Column(name = "registered_at", nullable = false)
  private Instant registeredAt;

  @ElementCollection
  @CollectionTable(
      name = "registration_option",
      joinColumns = @JoinColumn(name = "registration_id"))
  @BatchSize(size = 100)
  private List<OptionRow> options = new ArrayList<>();

  @ElementCollection
  @CollectionTable(
      name = "registration_consent",
      joinColumns = @JoinColumn(name = "registration_id"))
  @BatchSize(size = 100)
  private List<ConsentRow> consents = new ArrayList<>();

  protected RegistrationEntity() {}

  RegistrationEntity(
      UUID id,
      RegistrationType type,
      String firstName,
      String lastName,
      String email,
      String organization,
      String studyInstitution,
      String studyProgramme,
      String studentId,
      Instant registeredAt,
      List<OptionRow> options,
      List<ConsentRow> consents) {
    this.id = id;
    this.type = type;
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.organization = organization;
    this.studyInstitution = studyInstitution;
    this.studyProgramme = studyProgramme;
    this.studentId = studentId;
    this.registeredAt = registeredAt;
    this.options = new ArrayList<>(options);
    this.consents = new ArrayList<>(consents);
  }

  UUID id() {
    return id;
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

  Instant registeredAt() {
    return registeredAt;
  }

  List<OptionRow> options() {
    return List.copyOf(options);
  }

  List<ConsentRow> consents() {
    return List.copyOf(consents);
  }

  /** A row of {@code registration_option}. */
  @Embeddable
  public static class OptionRow {

    @Column(name = "option_id", nullable = false)
    private String optionId;

    @Column(name = "option_name", nullable = false)
    private String optionName;

    @Enumerated(EnumType.STRING)
    @Column(name = "option_category", nullable = false)
    private OptionCategory optionCategory;

    protected OptionRow() {}

    OptionRow(String optionId, String optionName, OptionCategory optionCategory) {
      this.optionId = optionId;
      this.optionName = optionName;
      this.optionCategory = optionCategory;
    }

    String optionId() {
      return optionId;
    }

    String optionName() {
      return optionName;
    }

    OptionCategory optionCategory() {
      return optionCategory;
    }
  }

  /** A row of {@code registration_consent}. */
  @Embeddable
  public static class ConsentRow {

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
