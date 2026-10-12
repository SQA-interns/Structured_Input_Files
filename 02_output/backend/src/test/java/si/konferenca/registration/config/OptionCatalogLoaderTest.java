package si.konferenca.registration.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import si.konferenca.registration.domain.Category;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.OptionCatalog;
import si.konferenca.registration.domain.RegistrationType;

class OptionCatalogLoaderTest {

  private static final String CONSENT = "\"consent\":{\"id\":\"c\",\"text\":\"I agree.\"}";

  private static OptionCatalog load(String json) {
    return OptionCatalogLoader.load(new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8)));
  }

  private static String file(String options) {
    return "{" + CONSENT + ",\"options\":[" + options + "]}";
  }

  @Test
  void loadsTheBundledLocalDefault() {
    OptionCatalog catalog =
        OptionCatalogLoader.load(new ClassPathResource("conference-options.json"));

    assertThat(catalog.consent().id()).isEqualTo("data-processing");
    assertThat(catalog.activeOptions()).isNotEmpty();
  }

  @Test
  void defaultsRegistrationTypesToBoth() {
    OptionCatalog catalog =
        load(file("{\"id\":\"ws\",\"name\":\"W\",\"category\":\"WORKSHOP\",\"active\":true}"));

    ConferenceOption option = catalog.find("ws").orElseThrow();
    assertThat(option.types()).containsExactlyInAnyOrder(RegistrationType.values());
    assertThat(option.category()).isEqualTo(Category.WORKSHOP);
  }

  static Stream<Arguments> invalidOptions() {
    return Stream.of(
        Arguments.of(
            "{\"id\":\"Bad Id\",\"name\":\"W\",\"category\":\"MEAL\",\"active\":true}",
            "id is missing or invalid"),
        Arguments.of(
            "{\"name\":\"W\",\"category\":\"MEAL\",\"active\":true}", "id is missing or invalid"),
        Arguments.of(
            "{\"id\":\"x\",\"name\":\" \",\"category\":\"MEAL\",\"active\":true}",
            "'x': name must have"),
        Arguments.of(
            "{\"id\":\"x\",\"name\":\"W\",\"category\":\"FOOD\",\"active\":true}",
            "'x': unknown category 'FOOD'"),
        Arguments.of(
            "{\"id\":\"x\",\"name\":\"W\",\"category\":\"MEAL\"}", "'x': 'active' is required"),
        Arguments.of(
            "{\"id\":\"x\",\"name\":\"W\",\"category\":\"MEAL\",\"active\":true,\"registrationTypes\":[]}",
            "'x': registrationTypes must not be empty"),
        Arguments.of(
            "{\"id\":\"x\",\"name\":\"W\",\"category\":\"MEAL\",\"active\":true,\"registrationTypes\":[\"ROBOT\"]}",
            "'x': invalid registration type 'ROBOT'"),
        Arguments.of(
            "{\"id\":\"x\",\"name\":\"W\",\"category\":\"MEAL\",\"active\":true,\"registrationTypes\":[\"STUDENT\",\"STUDENT\"]}",
            "'x': invalid registration type 'STUDENT'"),
        Arguments.of(
            "{\"id\":\"x\",\"name\":\"W\",\"category\":\"MEAL\",\"active\":true},{\"id\":\"x\",\"name\":\"V\",\"category\":\"MEAL\",\"active\":true}",
            "option[1] 'x': duplicate id"));
  }

  @ParameterizedTest
  @MethodSource("invalidOptions")
  void rejectsInvalidOptionsNamingTheEntry(String options, String message) {
    assertThatThrownBy(() -> load(file(options))).hasMessageContaining(message);
  }

  @Test
  void rejectsAnInvalidConsentOrShape() {
    assertThatThrownBy(() -> load("{\"options\":[]}")).hasMessageContaining("'consent'");
    assertThatThrownBy(() -> load("{\"consent\":{\"id\":\"C!\",\"text\":\"t\"},\"options\":[]}"))
        .hasMessageContaining("Consent id");
    assertThatThrownBy(() -> load("{\"consent\":{\"id\":\"c\",\"text\":\"\"},\"options\":[]}"))
        .hasMessageContaining("Consent text");
    assertThatThrownBy(() -> load(file("").replace("]}", "],\"extra\":1}")))
        .hasMessageContaining("Cannot read conference options");
    assertThatThrownBy(() -> load("not json")).hasMessageContaining("Cannot read");
  }
}
