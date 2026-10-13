package si.konferenca.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DomainModelTest {

  @Test
  void labels() {
    assertThat(RegistrationType.EXTERNAL.label()).isEqualTo("External participant");
    assertThat(RegistrationType.STUDENT.label()).isEqualTo("Student");
    assertThat(OptionCategory.WORKSHOP.label()).isEqualTo("Workshop");
    assertThat(OptionCategory.EVENT.label()).isEqualTo("Event");
    assertThat(OptionCategory.MEAL.label()).isEqualTo("Meal");
    assertThat(OptionCategory.OTHER.label()).isEqualTo("Other activity");
  }

  @Test
  void optionIsSelectableOnlyWhenActiveAndAvailableToTheType() {
    Set<RegistrationType> externalOnly = EnumSet.of(RegistrationType.EXTERNAL);
    ConferenceOption active =
        new ConferenceOption("a", "A", OptionCategory.EVENT, true, externalOnly);
    ConferenceOption inactive =
        new ConferenceOption("b", "B", OptionCategory.EVENT, false, externalOnly);

    assertThat(active.selectableBy(RegistrationType.EXTERNAL)).isTrue();
    assertThat(active.selectableBy(RegistrationType.STUDENT)).isFalse();
    assertThat(inactive.selectableBy(RegistrationType.EXTERNAL)).isFalse();
    assertThatThrownBy(() -> active.availableTo().add(RegistrationType.STUDENT))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void catalogFindsOptionsConsentsAndActiveOptions() {
    ConferenceOption a =
        new ConferenceOption(
            "a", "A", OptionCategory.MEAL, true, EnumSet.allOf(RegistrationType.class));
    ConferenceOption b =
        new ConferenceOption(
            "b", "B", OptionCategory.MEAL, false, EnumSet.allOf(RegistrationType.class));
    ConsentDefinition c = new ConsentDefinition("c", "C", true);
    List<ConferenceOption> options = new ArrayList<>(List.of(a, b));
    ConferenceCatalog catalog = new ConferenceCatalog(options, List.of(c));
    options.clear();

    assertThat(catalog.options()).containsExactly(a, b);
    assertThat(catalog.activeOptions()).containsExactly(a);
    assertThat(catalog.option("b")).contains(b);
    assertThat(catalog.option("x")).isEmpty();
    assertThat(catalog.consent("c")).contains(c);
    assertThat(catalog.consent("x")).isEmpty();
  }

  @Test
  void fieldErrorsKeepTheFirstMessagePerFieldInOrder() {
    FieldErrors errors = new FieldErrors();
    assertThat(errors.isEmpty()).isTrue();

    errors.add("b", "first");
    errors.add("b", "second");
    errors.addAll(Map.of("a", "other"));

    assertThat(errors.isEmpty()).isFalse();
    assertThat(errors.asMap()).containsExactly(Map.entry("b", "first"), Map.entry("a", "other"));
    assertThatThrownBy(() -> errors.asMap().put("c", "x"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void submissionListsAreCopiesAndMayBeNull() {
    List<String> ids = new ArrayList<>(List.of("x"));
    RegistrationSubmission s =
        new RegistrationSubmission("EXTERNAL", "a", "b", "c", "d", null, null, null, ids, null);
    ids.add("y");

    assertThat(s.optionIds()).containsExactly("x");
    assertThat(s.consents()).isNull();
    assertThatThrownBy(() -> s.optionIds().add("z"))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
