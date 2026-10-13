package si.konferenca.registration.adapter.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Table `registration` with its option and consent rows (`database.sql`). */
@Entity
@Table(name = "registration")
public class RegistrationEntity {

  @Id private UUID id;

  @Column(name = "type", nullable = false)
  private String type;

  @Column(name = "first_name", nullable = false)
  private String firstName;

  @Column(name = "last_name", nullable = false)
  private String lastName;

  @Column(name = "email", nullable = false)
  private String email;

  @Column(name = "organization")
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
  @OrderColumn(name = "position")
  private List<OptionRow> options = new ArrayList<>();

  @ElementCollection
  @CollectionTable(
      name = "registration_consent",
      joinColumns = @JoinColumn(name = "registration_id"))
  private List<ConsentRow> consents = new ArrayList<>();

  /** For JPA. */
  protected RegistrationEntity() {
    // required by JPA
  }

  RegistrationEntity(
      UUID id,
      String type,
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

  String type() {
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

  /** A row of `registration_option`. */
  @Embeddable
  public static class OptionRow {

    @Column(name = "option_id", nullable = false)
    private String optionId;

    @Column(name = "option_name", nullable = false)
    private String optionName;

    @Column(name = "category", nullable = false)
    private String category;

    protected OptionRow() {
      // required by JPA
    }

    OptionRow(String optionId, String optionName, String category) {
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

    String category() {
      return category;
    }
  }

  /** A row of `registration_consent`. */
  @Embeddable
  public static class ConsentRow {

    @Column(name = "consent_id", nullable = false)
    private String consentId;

    @Column(name = "consent_text", nullable = false)
    private String consentText;

    @Column(name = "given_at", nullable = false)
    private Instant givenAt;

    protected ConsentRow() {
      // required by JPA
    }

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
