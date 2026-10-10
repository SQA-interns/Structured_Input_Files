package si.konferenca.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DomainModelTest {

  @Test
  void optionWithoutAudienceIsForEveryoneButOnlyWhenActive() {
    ConferenceOption open = new ConferenceOption("a", "A", OptionCategory.MEAL, true, null);
    ConferenceOption inactive =
        new ConferenceOption("b", "B", OptionCategory.MEAL, false, Set.of());
    ConferenceOption students =
        new ConferenceOption(
            "c", "C", OptionCategory.EVENT, true, Set.of(RegistrationType.STUDENT));

    assertThat(open.audience()).containsExactlyInAnyOrder(RegistrationType.values());
    assertThat(open.selectableBy(RegistrationType.EXTERNAL)).isTrue();
    assertThat(inactive.selectableBy(RegistrationType.STUDENT)).isFalse();
    assertThat(students.selectableBy(RegistrationType.STUDENT)).isTrue();
    assertThat(students.selectableBy(RegistrationType.EXTERNAL)).isFalse();
  }

  @Test
  void catalogFindsOptionsAndConsentsAndListsActiveOnes() {
    OptionCatalog catalog = Catalogs.sample();

    assertThat(catalog.activeOptions())
        .extracting(ConferenceOption::id)
        .containsExactly("ws-ai", "ev-gala", "ev-night", "meal-lunch");
    assertThat(catalog.option("ws-old")).isPresent();
    assertThat(catalog.option("none")).isEmpty();
    assertThat(catalog.consent("photos")).map(Consent::mandatory).contains(false);
    assertThat(catalog.consent("none")).isEmpty();
  }

  @Test
  void catalogIsImmutable() {
    List<Consent> consents = new ArrayList<>(List.of(new Consent("a", "A", true)));
    OptionCatalog catalog = new OptionCatalog(List.of(), consents);
    consents.clear();

    assertThat(catalog.consents()).hasSize(1);
  }

  @Test
  void normalizedEmailIsTrimmedAndLowerCase() {
    AcceptedRegistration r =
        new AcceptedRegistration(
            UUID.randomUUID(),
            Instant.EPOCH,
            RegistrationType.EXTERNAL,
            new AcceptedRegistration.Participant(
                "A", "B", " Ana@Example.SI ", "O", null, null, null),
            List.of(),
            List.of());

    assertThat(r.normalizedEmail()).isEqualTo("ana@example.si");
  }

  @Test
  void labels() {
    assertThat(RegistrationType.STUDENT.label()).isEqualTo("Student");
    assertThat(RegistrationType.EXTERNAL.label()).isEqualTo("External participant");
    assertThat(OptionCategory.OTHER.label()).isEqualTo("Other activity");
    assertThat(OptionCategory.WORKSHOP.pluralLabel()).isEqualTo("Workshops");
  }

  @Test
  void formDefaultsMissingListsToEmpty() {
    RegistrationForm form =
        new RegistrationForm(null, null, null, null, null, null, null, null, null, null, null);

    assertThat(form.optionIds()).isEmpty();
    assertThat(form.consentIds()).isEmpty();
  }
}
