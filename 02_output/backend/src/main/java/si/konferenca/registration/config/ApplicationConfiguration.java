package si.konferenca.registration.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import si.konferenca.registration.application.FormSettings;
import si.konferenca.registration.domain.ConferenceCatalogue;
import si.konferenca.registration.domain.InvalidConfigurationException;

/** Beans built from configuration (spec §3). */
@Configuration
public class ApplicationConfiguration {

  /** The options and consents, read once at start-up from CONFERENCE_CONFIG (AR-04). */
  @Bean
  public ConferenceCatalogue conferenceCatalogue(
      ResourceLoader resourceLoader, @Value("${app.conference.config-location}") String location) {
    Resource resource = resourceLoader.getResource(location);
    if (!resource.exists()) {
      throw new InvalidConfigurationException(
          "Conference configuration " + location + " does not exist");
    }
    try {
      return ConferenceConfigLoader.load(resource.getInputStream(), location);
    } catch (IOException e) {
      throw new UncheckedIOException("Conference configuration " + location + " is unreadable", e);
    }
  }

  @Bean
  public FormSettings formSettings(
      @Value("${app.conference.name}") String conferenceName,
      @Value("${app.recaptcha.test-mode}") boolean captchaTestMode,
      @Value("${app.recaptcha.site-key}") String siteKey) {
    return new FormSettings(conferenceName, captchaTestMode, siteKey);
  }

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
