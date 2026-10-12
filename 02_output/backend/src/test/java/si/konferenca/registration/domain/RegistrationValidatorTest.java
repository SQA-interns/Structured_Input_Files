package si.konferenca.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RegistrationValidatorTest {

  private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");
  private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final OptionCatalog CATALOG =
      new OptionCatalog(
          new Consent("c", "I agree."),
          List.of(
              new ConferenceOption(
                  "ws", "Workshop", Category.WORKSHOP, true, EnumSet.allOf(RegistrationType.class)),
              new ConferenceOption(
                  "ext", "Pro", Category.WORKSHOP, true, EnumSet.of(RegistrationType.EXTERNAL)),
              new ConferenceOption(
                  "old", "Old", Category.EVENT, false, EnumSet.allOf(RegistrationType.class))));

  private final RegistrationValidator validator = new RegistrationValidator(CATALOG);

  private static RegistrationForm external(String first, String email, List<String> options) {
    return new RegistrationForm(
        "EXTERNAL", first, "Novak", email, "Acme", null, null, null, options, true);
  }

  private static RegistrationForm student(String institution, List<String> options) {
    return new RegistrationForm(
        "STUDENT", "Luka", "Kranjc", "luka@x.si", null, institution, "CS", "123", options, true);
  }

  private List<FieldError> errors(RegistrationForm form) {
    ValidationException e =
        catchThrowableOfType(ValidationException.class, () -> validator.validate(form, ID, NOW));
    assertThat(e).as("expected a validation failure").isNotNull();
    return e.errors();
  }

  private static List<String> fields(List<FieldError> errors) {
    return errors.stream().map(FieldError::field).toList();
  }

  @Test
  void acceptsAndTrimsAValidExternalRegistration() {
    Registration r =
        validator.validate(
            new RegistrationForm(
                "EXTERNAL",
                " Ana ",
                "Novak",
                " ana@x.si ",
                " Acme ",
                "",
                "  ",
                null,
                List.of("ws", "ext"),
                true),
            ID,
            NOW);

    assertThat(r.id()).isEqualTo(ID);
    assertThat(r.type()).isEqualTo(RegistrationType.EXTERNAL);
    assertThat(r.firstName()).isEqualTo("Ana");
    assertThat(r.email()).isEqualTo("ana@x.si");
    assertThat(r.organization()).isEqualTo("Acme");
    assertThat(r.studyInstitution()).isNull();
    assertThat(r.selectedOptions())
        .extracting(SelectedOption::id, SelectedOption::name, SelectedOption::category)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("ws", "Workshop", Category.WORKSHOP),
            org.assertj.core.groups.Tuple.tuple("ext", "Pro", Category.WORKSHOP));
    assertThat(r.consent()).isEqualTo(new ConsentRecord("c", "I agree.", NOW));
    assertThat(r.submittedAt()).isEqualTo(NOW);
  }

  @Test
  void acceptsAValidStudentRegistration() {
    Registration r = validator.validate(student("Uni", List.of("ws")), ID, NOW);

    assertThat(r.type()).isEqualTo(RegistrationType.STUDENT);
    assertThat(r.studyInstitution()).isEqualTo("Uni");
    assertThat(r.studyProgramme()).isEqualTo("CS");
    assertThat(r.studentId()).isEqualTo("123");
    assertThat(r.organization()).isNull();
  }

  @Test
  void reportsEveryMissingFieldTogether() {
    RegistrationForm form =
        new RegistrationForm("EXTERNAL", " ", null, "", "", null, null, null, List.of(), false);

    assertThat(fields(errors(form)))
        .containsExactlyInAnyOrder(
            "firstName", "lastName", "email", "organization", "consentGiven");
    assertThat(errors(form))
        .filteredOn(e -> e.field().equals("firstName"))
        .extracting(FieldError::message)
        .containsExactly(RegistrationValidator.REQUIRED);
  }

  @Test
  void rejectsAnUnknownType() {
    RegistrationForm form =
        new RegistrationForm("ROBOT", "A", "B", "a@b.si", "X", null, null, null, List.of(), true);

    assertThat(fields(errors(form))).containsExactly("type");
  }

  @Test
  void rejectsAMissingTypeButStillChecksTheOtherFields() {
    RegistrationForm form =
        new RegistrationForm(null, "", "B", "a@b.si", null, null, null, null, List.of(), true);

    assertThat(fields(errors(form))).containsExactlyInAnyOrder("type", "firstName");
  }

  @Test
  void rejectsFieldsOfTheOtherType() {
    RegistrationForm external =
        new RegistrationForm(
            "EXTERNAL", "A", "B", "a@b.si", "X", "Uni", null, null, List.of(), true);
    RegistrationForm student =
        new RegistrationForm(
            "STUDENT", "A", "B", "a@b.si", "Acme", "Uni", "CS", "1", List.of(), true);

    assertThat(errors(external))
        .containsExactly(new FieldError("studyInstitution", RegistrationValidator.NOT_ALLOWED));
    assertThat(errors(student))
        .containsExactly(new FieldError("organization", RegistrationValidator.NOT_ALLOWED));
  }

  @Test
  void requiresTheStudentFields() {
    assertThat(fields(errors(student("  ", List.of())))).containsExactly("studyInstitution");
  }

  @ParameterizedTest
  @ValueSource(strings = {"a", "a@b", "@b.si", "a@.si", "a b@c.si", "a@b c.si", "a@@b.si"})
  void rejectsInvalidEmails(String email) {
    assertThat(errors(external("Ana", email, List.of())))
        .containsExactly(new FieldError("email", RegistrationValidator.BAD_EMAIL));
  }

  @Test
  void rejectsControlCharactersInTheEmail() {
    assertThat(fields(errors(external("Ana", "a@b.si\r\nBcc: x@y.si", List.of()))))
        .containsExactly("email");
  }

  @Test
  void acceptsLineBreaksAndTabsInTextButNotOtherControlCharacters() {
    Registration r = validator.validate(external("A\r\nB\tC", "a@b.si", List.of()), ID, NOW);
    assertThat(r.firstName()).isEqualTo("A\r\nB\tC");

    assertThat(errors(external("A\u0000B", "a@b.si", List.of())))
        .containsExactly(new FieldError("firstName", RegistrationValidator.BAD_CHARACTERS));
    assertThat(fields(errors(external("A\u007fB", "a@b.si", List.of()))))
        .containsExactly("firstName");
  }

  @Test
  void enforcesTheLengthLimits() {
    String text200 = "x".repeat(200);
    assertThat(validator.validate(external(text200, "a@b.si", List.of()), ID, NOW).firstName())
        .hasSize(200);
    assertThat(errors(external(text200 + "x", "a@b.si", List.of())))
        .containsExactly(new FieldError("firstName", "Must be at most 200 characters."));

    String email254 = "a".repeat(248) + "@b.com";
    assertThat(validator.validate(external("A", email254, List.of()), ID, NOW).email())
        .hasSize(254);
    assertThat(errors(external("A", "a" + email254, List.of())))
        .containsExactly(new FieldError("email", "Must be at most 254 characters."));
  }

  @Test
  void rejectsUnknownInactiveUnavailableAndRepeatedOptions() {
    assertThat(errors(external("A", "a@b.si", List.of("nope"))))
        .containsExactly(
            new FieldError("optionIds", "Unknown, inactive or unavailable option: nope"));
    assertThat(errors(external("A", "a@b.si", List.of("old"))))
        .containsExactly(
            new FieldError("optionIds", "Unknown, inactive or unavailable option: old"));
    assertThat(errors(student("Uni", List.of("ext"))))
        .containsExactly(
            new FieldError("optionIds", "Unknown, inactive or unavailable option: ext"));
    assertThat(errors(external("A", "a@b.si", List.of("ws", "ws"))))
        .containsExactly(new FieldError("optionIds", "Option selected more than once: ws"));
  }

  @Test
  void doesNotEchoOptionIdsThatAreNotIdentifiers() {
    List<String> withNull = new java.util.ArrayList<>();
    withNull.add(null);
    assertThat(errors(external("A", "a@b.si", List.of("<script>"))))
        .containsExactly(new FieldError("optionIds", "Unknown option."));
    assertThat(errors(external("A", "a@b.si", withNull)))
        .containsExactly(new FieldError("optionIds", "Unknown option."));
  }

  @Test
  void requiresTheOptionList() {
    assertThat(errors(external("A", "a@b.si", null)))
        .containsExactly(new FieldError("optionIds", RegistrationValidator.REQUIRED));
  }

  @Test
  void requiresConsentToBeTrue() {
    RegistrationForm noConsent =
        new RegistrationForm(
            "EXTERNAL", "A", "B", "a@b.si", "X", null, null, null, List.of(), null);

    assertThat(errors(noConsent))
        .containsExactly(new FieldError("consentGiven", RegistrationValidator.CONSENT_REQUIRED));
  }
}
