package si.konferenca.registration.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import si.konferenca.registration.domain.ConferenceConfiguration;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.InvalidConfigurationException;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.RegistrationType;

class ConferenceConfigurationLoaderTest {

  @TempDir Path dir;

  private final ConferenceConfigurationLoader loader = new ConferenceConfigurationLoader();

  private Path write(String json) throws Exception {
    Path file = dir.resolve("conference.json");
    Files.writeString(file, json, StandardCharsets.UTF_8);
    return file;
  }

  @Test
  void AC_003_02_loadsNameConsentsAndOptionsFromFile() throws Exception {
    Path file =
        write(
            """
            {"conferenceName": "Konferenca Čšž",
             "consents": [{"id": "privacy", "text": "Strinjam se.", "mandatory": true}],
             "options": [
               {"id": "ws-a", "name": "Delavnica Ž", "category": "WORKSHOP", "active": true},
               {"id": "oth-b", "name": "Tour", "category": "OTHER", "active": false,
                "availableTo": ["STUDENT"]}]}
            """);

    ConferenceConfiguration config = loader.load(file);

    assertThat(config.conferenceName()).isEqualTo("Konferenca Čšž");
    assertThat(config.consents()).hasSize(1);
    assertThat(config.consents().get(0).text()).isEqualTo("Strinjam se.");
    assertThat(config.consents().get(0).mandatory()).isTrue();
    ConferenceOption first = config.options().get(0);
    assertThat(first.id()).isEqualTo("ws-a");
    assertThat(first.name()).isEqualTo("Delavnica Ž");
    assertThat(first.category()).isEqualTo(OptionCategory.WORKSHOP);
    assertThat(first.active()).isTrue();
    assertThat(first.availableTo())
        .containsExactlyInAnyOrder(RegistrationType.EXTERNAL, RegistrationType.STUDENT);
    ConferenceOption second = config.options().get(1);
    assertThat(second.active()).isFalse();
    assertThat(second.availableTo()).containsExactly(RegistrationType.STUDENT);
  }

  @Test
  void AC_003_03_unknownCategoryIsAConfigurationError() throws Exception {
    Path file =
        write(
            """
            {"conferenceName": "C", "consents": [],
             "options": [{"id": "x", "name": "X", "category": "PARTY", "active": true}]}
            """);

    assertThatThrownBy(() -> loader.load(file))
        .isInstanceOf(InvalidConfigurationException.class)
        .hasMessageStartingWith("Invalid conference configuration:");
  }

  @Test
  void AC_003_03_unknownRegistrationTypeIsAConfigurationError() throws Exception {
    Path file =
        write(
            """
            {"conferenceName": "C", "consents": [],
             "options": [{"id": "x", "name": "X", "category": "MEAL", "active": true,
                          "availableTo": ["GUEST"]}]}
            """);

    assertThatThrownBy(() -> loader.load(file)).isInstanceOf(InvalidConfigurationException.class);
  }

  @Test
  void AC_003_03_missingNameIsAConfigurationError() throws Exception {
    Path file =
        write(
            """
            {"conferenceName": "C", "consents": [],
             "options": [{"id": "x", "category": "MEAL", "active": true}]}
            """);

    assertThatThrownBy(() -> loader.load(file)).isInstanceOf(InvalidConfigurationException.class);
  }

  @Test
  void AC_003_03_unknownPropertyMalformedJsonOrMissingFileAreConfigurationErrors()
      throws Exception {
    Path unknownProperty =
        write("{\"conferenceName\": \"C\", \"consents\": [], \"options\": [], \"extra\": 1}");
    assertThatThrownBy(() -> loader.load(unknownProperty))
        .isInstanceOf(InvalidConfigurationException.class);

    Path malformed = write("{\"conferenceName\": ");
    assertThatThrownBy(() -> loader.load(malformed))
        .isInstanceOf(InvalidConfigurationException.class);

    assertThatThrownBy(() -> loader.load(dir.resolve("missing.json")))
        .isInstanceOf(InvalidConfigurationException.class)
        .hasMessageContaining("missing.json");
  }

  @Test
  void AC_003_03_missingListsOrActiveFlagAreConfigurationErrors() throws Exception {
    Path noOptions = write("{\"conferenceName\": \"C\", \"consents\": []}");
    assertThatThrownBy(() -> loader.load(noOptions))
        .isInstanceOf(InvalidConfigurationException.class);

    Path noActive =
        write(
            "{\"conferenceName\": \"C\", \"consents\": [],"
                + " \"options\": [{\"id\": \"x\", \"name\": \"X\", \"category\": \"MEAL\"}]}");
    assertThatThrownBy(() -> loader.load(noActive))
        .isInstanceOf(InvalidConfigurationException.class);
  }
}
