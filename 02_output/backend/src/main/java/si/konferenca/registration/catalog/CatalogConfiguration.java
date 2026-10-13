package si.konferenca.registration.catalog;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import si.konferenca.registration.domain.ConferenceCatalog;

/** Loads the conference configuration once at startup; an invalid file stops the start. */
@Configuration
public class CatalogConfiguration {

  @Bean
  public ConferenceCatalog conferenceCatalog(
      ResourceLoader resourceLoader, @Value("${registration.conference-config}") String location) {
    Resource resource = resourceLoader.getResource(location);
    if (!resource.exists()) {
      throw new InvalidConferenceConfigurationException(
          "Conference configuration not found: " + location);
    }
    try (InputStream in = resource.getInputStream()) {
      return new ConferenceCatalogParser().parse(in.readAllBytes());
    } catch (IOException e) {
      throw new InvalidConferenceConfigurationException(
          "Conference configuration cannot be read: " + location, e);
    }
  }
}
