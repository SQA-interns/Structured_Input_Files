package si.konferenca.registration.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import si.konferenca.registration.adapter.catalog.JsonFileConferenceCatalog;
import si.konferenca.registration.domain.ConferenceCatalog;
import si.konferenca.registration.domain.FormSettings;

/** Wires the ports to their adapters. */
@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class ApplicationConfig {

  @Bean
  ConferenceCatalog conferenceCatalog(AppProperties props) {
    String file = props.conference().configFile();
    return JsonFileConferenceCatalog.load(file == null || file.isBlank() ? null : Path.of(file));
  }

  @Bean
  FormSettings formSettings(AppProperties props) {
    return new FormSettings(
        props.conference().name(), props.recaptcha().testMode(), props.recaptcha().siteKey());
  }
}
