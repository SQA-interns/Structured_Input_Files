package si.konferenca.registration.conference;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import si.konferenca.registration.config.AppProperties;

/** Loads the options configuration once at startup from {@code CONFERENCE_CONFIG} (AR-04). */
@Configuration
public class ConferenceConfigurationLoader {

  @Bean
  ConferenceConfiguration conferenceConfiguration(
      AppProperties properties, ResourceLoader resourceLoader) {
    String location = properties.conference().config();
    Resource resource = resourceLoader.getResource(location);
    if (!resource.exists()) {
      throw new InvalidConferenceConfigurationException("the file " + location + " does not exist");
    }
    try (InputStream in = resource.getInputStream()) {
      return ConferenceConfigurationParser.parse(
          new String(in.readAllBytes(), StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new InvalidConferenceConfigurationException(
          "the file " + location + " cannot be read", e);
    }
  }
}
