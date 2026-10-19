package si.konferenca.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ConferenceConfigurationTest {

  private static final ConsentDefinition PRIVACY =
      new ConsentDefinition("privacy", "I agree.", true);

  private static ConferenceOption option(String id, boolean active) {
    return new ConferenceOption(id, "Name " + id, OptionCategory.WORKSHOP, active, null);
  }

  @Test
  void AC_003_01_activeOptionsKeepConfigurationOrderAndSkipInactive() {
    ConferenceConfiguration config =
        ConferenceConfiguration.of(
            "Conf",
            List.of(PRIVACY),
            List.of(option("b", true), option("a", false), option("c", true)));

    assertThat(config.activeOptions()).extracting(ConferenceOption::id).containsExactly("b", "c");
    assertThat(config.options()).hasSize(3);
    assertThat(config.conferenceName()).isEqualTo("Conf");
    assertThat(config.consents()).containsExactly(PRIVACY);
  }

  @Test
  void AC_003_01_optionIsFoundById() {
    ConferenceConfiguration config =
        ConferenceConfiguration.of("Conf", List.of(PRIVACY), List.of(option("a", false)));

    assertThat(config.option("a")).isPresent();
    assertThat(config.option("x")).isEmpty();
  }

  @Test
  void AC_003_01_availabilityDefaultsToBothTypes() {
    ConferenceOption defaulted = option("a", true);
    ConferenceOption emptyList =
        new ConferenceOption("c", "C", OptionCategory.OTHER, true, Set.of());
    ConferenceOption externalOnly =
        new ConferenceOption(
            "b", "B", OptionCategory.OTHER, true, Set.of(RegistrationType.EXTERNAL));

    assertThat(defaulted.availableTo())
        .containsExactlyInAnyOrder(RegistrationType.EXTERNAL, RegistrationType.STUDENT);
    assertThat(emptyList.availableTo())
        .containsExactlyInAnyOrder(RegistrationType.EXTERNAL, RegistrationType.STUDENT);
    assertThat(defaulted.isAvailableTo(RegistrationType.STUDENT)).isTrue();
    assertThat(externalOnly.isAvailableTo(RegistrationType.EXTERNAL)).isTrue();
    assertThat(externalOnly.isAvailableTo(RegistrationType.STUDENT)).isFalse();
  }

  @Test
  void AC_003_03_duplicateOptionIdIsRejected() {
    assertThatThrownBy(
            () ->
                ConferenceConfiguration.of(
                    "Conf", List.of(PRIVACY), List.of(option("a", true), option("a", false))))
        .isInstanceOf(InvalidConfigurationException.class)
        .hasMessageStartingWith("Invalid conference configuration:")
        .hasMessageContaining("'a'");
  }

  @Test
  void AC_003_03_duplicateConsentIdIsRejected() {
    assertThatThrownBy(
            () -> ConferenceConfiguration.of("Conf", List.of(PRIVACY, PRIVACY), List.of()))
        .isInstanceOf(InvalidConfigurationException.class)
        .hasMessageContaining("privacy");
  }

  @Test
  void AC_003_03_blankOrMissingNamesAreRejected() {
    ConferenceOption unnamed = new ConferenceOption("a", " ", OptionCategory.MEAL, true, null);
    ConferenceOption nullName = new ConferenceOption("b", null, OptionCategory.MEAL, true, null);
    ConsentDefinition noText = new ConsentDefinition("privacy", "", true);

    assertThatThrownBy(() -> ConferenceConfiguration.of("Conf", List.of(), List.of(unnamed)))
        .isInstanceOf(InvalidConfigurationException.class);
    assertThatThrownBy(() -> ConferenceConfiguration.of("Conf", List.of(), List.of(nullName)))
        .isInstanceOf(InvalidConfigurationException.class);
    assertThatThrownBy(() -> ConferenceConfiguration.of("Conf", List.of(noText), List.of()))
        .isInstanceOf(InvalidConfigurationException.class);
    assertThatThrownBy(() -> ConferenceConfiguration.of(" ", List.of(), List.of()))
        .isInstanceOf(InvalidConfigurationException.class);
    assertThatThrownBy(() -> ConferenceConfiguration.of(null, List.of(), List.of()))
        .isInstanceOf(InvalidConfigurationException.class);
  }

  @Test
  void AC_003_03_missingCategoryOrIdIsRejected() {
    ConferenceOption noCategory = new ConferenceOption("a", "A", null, true, null);
    ConferenceOption noId = new ConferenceOption(" ", "A", OptionCategory.MEAL, true, null);
    ConsentDefinition noConsentId = new ConsentDefinition(null, "Text", true);

    assertThatThrownBy(() -> ConferenceConfiguration.of("Conf", List.of(), List.of(noCategory)))
        .isInstanceOf(InvalidConfigurationException.class);
    assertThatThrownBy(() -> ConferenceConfiguration.of("Conf", List.of(), List.of(noId)))
        .isInstanceOf(InvalidConfigurationException.class);
    assertThatThrownBy(() -> ConferenceConfiguration.of("Conf", List.of(noConsentId), List.of()))
        .isInstanceOf(InvalidConfigurationException.class);
  }
}
