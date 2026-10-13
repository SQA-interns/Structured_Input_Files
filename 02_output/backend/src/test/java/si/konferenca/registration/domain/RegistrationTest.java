package si.konferenca.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The accepted registration and the catalogue (BR-01, BR-04, BR-05, SB-14). */
class RegistrationTest {

  private static final ConferenceCatalogue CATALOGUE =
      new ConferenceCatalogue(
          List.of(
              new ConferenceOption(
                  "ws",
                  "Workshop",
                  OptionCategory.WORKSHOP,
                  true,
                  EnumSet.allOf(RegistrationType.class)),
              new ConferenceOption(
                  "ot", "Tour", OptionCategory.OTHER, true, EnumSet.of(RegistrationType.STUDENT))),
          List.of(
              new ConsentDefinition("privacy", "I agree", true),
              new ConsentDefinition("photos", "Photos ok", false)));

  private static final Instant AT = Instant.parse("2026-10-13T20:15:03Z");

  @Test
  void studentRegistrationKeepsOnlyStudentFieldsAndGivenConsents() {
    Submission s =
        new Submission(
            "STUDENT",
            "Luka",
            "Kranjc",
            "l@x.si",
            "ignored",
            "Uni",
            "CS",
            "63",
            List.of("ot", "ws"),
            Map.of("privacy", true, "photos", false),
            "t");
    UUID id = UUID.randomUUID();

    Registration r = Registration.accept(s, CATALOGUE, id, AT);

    assertThat(r.id()).isEqualTo(id);
    assertThat(r.type()).isEqualTo(RegistrationType.STUDENT);
    assertThat(r.organization()).isNull();
    assertThat(r.studyInstitution()).isEqualTo("Uni");
    assertThat(r.studyProgramme()).isEqualTo("CS");
    assertThat(r.studentId()).isEqualTo("63");
    assertThat(r.options())
        .containsExactly(
            new Registration.SelectedOption("ot", "Tour", OptionCategory.OTHER),
            new Registration.SelectedOption("ws", "Workshop", OptionCategory.WORKSHOP));
    assertThat(r.consents())
        .containsExactly(new Registration.GivenConsent("privacy", "I agree", AT));
  }

  @Test
  void externalRegistrationKeepsOnlyTheOrganization() {
    Submission s =
        new Submission(
            "EXTERNAL",
            "Ana",
            "Novak",
            "a@x.si",
            "Org",
            "Uni",
            "CS",
            "63",
            List.of(),
            Map.of("privacy", true, "photos", true),
            "t");

    Registration r = Registration.accept(s, CATALOGUE, UUID.randomUUID(), AT);

    assertThat(r.organization()).isEqualTo("Org");
    assertThat(r.studyInstitution()).isNull();
    assertThat(r.studyProgramme()).isNull();
    assertThat(r.studentId()).isNull();
    assertThat(r.consents())
        .extracting(Registration.GivenConsent::id)
        .containsExactly("privacy", "photos");
  }

  @Test
  void timesAreFormattedInUtcWithMilliseconds() {
    assertThat(Registration.format(AT)).isEqualTo("2026-10-13T20:15:03.000Z");
    assertThat(Registration.format(Instant.parse("2026-01-02T03:04:05.678Z")))
        .isEqualTo("2026-01-02T03:04:05.678Z");
  }

  @Test
  void catalogueRejectsDuplicateConsentsAndFindsEntries() {
    assertThatThrownBy(
            () ->
                new ConferenceCatalogue(
                    List.of(),
                    List.of(
                        new ConsentDefinition("p", "a", true),
                        new ConsentDefinition("p", "b", false))))
        .isInstanceOf(InvalidConfigurationException.class)
        .hasMessage("Duplicate consent id 'p'");
    assertThat(CATALOGUE.option("ws")).isPresent();
    assertThat(CATALOGUE.option("none")).isEmpty();
  }

  @Test
  void optionsAreSelectableOnlyWhenActiveAndAvailable() {
    ConferenceOption inactive =
        new ConferenceOption(
            "x", "X", OptionCategory.MEAL, false, EnumSet.allOf(RegistrationType.class));
    assertThat(inactive.selectableBy(RegistrationType.EXTERNAL)).isFalse();
    assertThat(CATALOGUE.option("ot").orElseThrow().selectableBy(RegistrationType.EXTERNAL))
        .isFalse();
    assertThat(CATALOGUE.option("ot").orElseThrow().selectableBy(RegistrationType.STUDENT))
        .isTrue();
  }

  @Test
  void enumsParseExactNamesAndCarryLabels() {
    assertThat(RegistrationType.parse("STUDENT")).contains(RegistrationType.STUDENT);
    assertThat(RegistrationType.parse("student")).isEmpty();
    assertThat(RegistrationType.parse(null)).isEmpty();
    assertThat(RegistrationType.EXTERNAL.label()).isEqualTo("External participant");
    assertThat(RegistrationType.STUDENT.label()).isEqualTo("Student");
    assertThat(OptionCategory.parse("MEAL")).contains(OptionCategory.MEAL);
    assertThat(OptionCategory.parse("meal")).isEmpty();
    assertThat(OptionCategory.OTHER.singular()).isEqualTo("Other activity");
    assertThat(OptionCategory.OTHER.plural()).isEqualTo("Other activities");
    assertThat(OptionCategory.WORKSHOP.singular()).isEqualTo("Workshop");
    assertThat(OptionCategory.EVENT.plural()).isEqualTo("Events");
  }

  @Test
  void configurationErrorKeepsItsCause() {
    var cause = new IllegalStateException("x");
    assertThat(new InvalidConfigurationException("m", cause)).hasMessage("m").hasCause(cause);
  }
}
