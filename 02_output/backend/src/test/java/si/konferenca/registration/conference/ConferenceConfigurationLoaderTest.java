package si.konferenca.registration.conference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import si.konferenca.registration.config.AppProperties;

class ConferenceConfigurationLoaderTest {

  private final ConferenceConfigurationLoader loader = new ConferenceConfigurationLoader();

  static AppProperties properties(String config) {
    return new AppProperties(
        "test",
        new AppProperties.Conference(config, "Conf"),
        new AppProperties.JsonCopy(java.nio.file.Path.of("copies")),
        new AppProperties.Recaptcha(true, "", "", "http://localhost"),
        new AppProperties.Organizer("org", "secret", List.of("o@example.org"), true),
        new AppProperties.Mail("from@example.org"),
        new AppProperties.Limits(1, 1, 1, 100),
        new AppProperties.Cors(""));
  }

  @Test
  void loadsTheShippedConfiguration() {
    ConferenceConfiguration configuration =
        loader.conferenceConfiguration(
            properties("classpath:conference.json"), new DefaultResourceLoader());

    assertThat(configuration.activeOptions()).isNotEmpty();
    assertThat(configuration.consents())
        .extracting(ConsentDefinition::id)
        .containsExactly("privacy");
  }

  @Test
  void refusesAMissingFile() {
    assertThatThrownBy(
            () ->
                loader.conferenceConfiguration(
                    properties("classpath:does-not-exist.json"), new DefaultResourceLoader()))
        .isInstanceOf(InvalidConferenceConfigurationException.class)
        .hasMessageContaining("does not exist");
  }

  @Test
  void refusesAnUnreadableFile() throws IOException {
    Resource resource = mock(Resource.class);
    when(resource.exists()).thenReturn(true);
    when(resource.getInputStream()).thenThrow(new IOException("broken"));
    ResourceLoader resourceLoader = mock(ResourceLoader.class);
    when(resourceLoader.getResource("file:broken.json")).thenReturn(resource);

    assertThatThrownBy(
            () -> loader.conferenceConfiguration(properties("file:broken.json"), resourceLoader))
        .isInstanceOf(InvalidConferenceConfigurationException.class)
        .hasMessageContaining("cannot be read")
        .hasCauseInstanceOf(IOException.class);
  }
}
