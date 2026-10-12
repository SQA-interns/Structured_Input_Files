package si.konferenca.registration.conference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import si.konferenca.registration.domain.RegistrationType;

class ConferenceConfigurationParserTest {

  private static final String CONSENTS =
      "\"consents\":[{\"id\":\"privacy\",\"text\":\"I agree.\",\"mandatory\":true}]";

  private static String config(String options) {
    return "{\"options\":[" + options + "]," + CONSENTS + "}";
  }

  @Test
  void parsesOptionsWithDefaults() {
    ConferenceConfiguration configuration =
        ConferenceConfigurationParser.parse(
            config(
                "{\"id\":\"ws-a\",\"name\":\" Workshop A \",\"category\":\"WORKSHOP\"},"
                    + "{\"id\":\"meal-b\",\"name\":\"Meal\",\"category\":\"MEAL\","
                    + "\"active\":false,\"availableTo\":[\"STUDENT\"]}"));

    ConferenceOption workshop = configuration.option("ws-a").orElseThrow();
    assertThat(workshop.name()).isEqualTo("Workshop A");
    assertThat(workshop.category()).isEqualTo(Category.WORKSHOP);
    assertThat(workshop.active()).isTrue();
    assertThat(workshop.availableTo()).isEqualTo(EnumSet.allOf(RegistrationType.class));
    ConferenceOption meal = configuration.option("meal-b").orElseThrow();
    assertThat(meal.active()).isFalse();
    assertThat(meal.availableTo()).containsExactly(RegistrationType.STUDENT);
    assertThat(configuration.activeOptions()).containsExactly(workshop);
    assertThat(configuration.option("missing")).isEmpty();
    assertThat(configuration.consents())
        .containsExactly(new ConsentDefinition("privacy", "I agree.", true));
  }

  @Test
  void selectableOnlyWhenActiveAndAvailable() {
    ConferenceOption active =
        new ConferenceOption("a", "A", Category.EVENT, true, EnumSet.of(RegistrationType.EXTERNAL));
    ConferenceOption inactive =
        new ConferenceOption(
            "b", "B", Category.EVENT, false, EnumSet.allOf(RegistrationType.class));

    assertThat(active.selectableBy(RegistrationType.EXTERNAL)).isTrue();
    assertThat(active.selectableBy(RegistrationType.STUDENT)).isFalse();
    assertThat(inactive.selectableBy(RegistrationType.EXTERNAL)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "not json",
        "[]",
        "{\"consents\":[]}",
        "{\"options\":{},\"consents\":[]}",
        "{\"options\":[],\"consents\":{}}",
        "{\"options\":[\"ws\"],\"consents\":[]}",
        "{\"options\":[],\"consents\":[{\"id\":\"p\",\"text\":\"t\",\"mandatory\":true},"
            + "{\"id\":\"p\",\"text\":\"t\",\"mandatory\":true}]}",
        "{\"options\":[],\"consents\":[{\"id\":\"p\",\"mandatory\":true}]}",
        "{\"options\":[],\"consents\":[{\"id\":\"p\",\"text\":\" \",\"mandatory\":true}]}",
        "{\"options\":[],\"consents\":[{\"id\":\"p\",\"text\":\"t\"}]}",
        "{\"options\":[],\"consents\":[{\"id\":\"p\",\"text\":\"t\",\"mandatory\":\"yes\"}]}",
        "{\"options\":[],\"consents\":[{\"text\":\"t\",\"mandatory\":true}]}"
      })
  void rejectsInvalidDocuments(String json) {
    assertThatThrownBy(() -> ConferenceConfigurationParser.parse(json))
        .isInstanceOf(InvalidConferenceConfigurationException.class)
        .hasMessageStartingWith("Invalid conference configuration: ");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{\"id\":\"ws\",\"name\":\"A\",\"category\":\"WORKSHOP\"},"
            + "{\"id\":\"ws\",\"name\":\"B\",\"category\":\"WORKSHOP\"}",
        "{\"name\":\"A\",\"category\":\"WORKSHOP\"}",
        "{\"id\":\"Upper\",\"name\":\"A\",\"category\":\"WORKSHOP\"}",
        "{\"id\":5,\"name\":\"A\",\"category\":\"WORKSHOP\"}",
        "{\"id\":\"ws\",\"category\":\"WORKSHOP\"}",
        "{\"id\":\"ws\",\"name\":7,\"category\":\"WORKSHOP\"}",
        "{\"id\":\"ws\",\"name\":\"  \",\"category\":\"WORKSHOP\"}",
        "{\"id\":\"ws\",\"name\":\"A\"}",
        "{\"id\":\"ws\",\"name\":\"A\",\"category\":3}",
        "{\"id\":\"ws\",\"name\":\"A\",\"category\":\"SPORT\"}",
        "{\"id\":\"ws\",\"name\":\"A\",\"category\":\"workshop\"}",
        "{\"id\":\"ws\",\"name\":\"A\",\"category\":\"WORKSHOP\",\"active\":\"yes\"}",
        "{\"id\":\"ws\",\"name\":\"A\",\"category\":\"WORKSHOP\",\"availableTo\":[]}",
        "{\"id\":\"ws\",\"name\":\"A\",\"category\":\"WORKSHOP\",\"availableTo\":\"STUDENT\"}",
        "{\"id\":\"ws\",\"name\":\"A\",\"category\":\"WORKSHOP\",\"availableTo\":[\"GUEST\"]}"
      })
  void rejectsInvalidOptions(String options) {
    assertThatThrownBy(() -> ConferenceConfigurationParser.parse(config(options)))
        .isInstanceOf(InvalidConferenceConfigurationException.class);
  }

  @Test
  void exceptionKeepsTheCause() {
    IllegalArgumentException cause = new IllegalArgumentException("x");

    assertThat(new InvalidConferenceConfigurationException("m", cause))
        .hasMessage("Invalid conference configuration: m")
        .hasCause(cause);
  }
}
