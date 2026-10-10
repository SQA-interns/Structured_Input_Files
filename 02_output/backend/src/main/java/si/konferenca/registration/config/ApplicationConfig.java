package si.konferenca.registration.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import si.konferenca.registration.application.FormConfigService;
import si.konferenca.registration.domain.OptionCatalog;
import si.konferenca.registration.infrastructure.InvalidOptionsException;
import si.konferenca.registration.infrastructure.OptionCatalogLoader;

/** Composition root: wires application services and adapters from configuration. */
@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class ApplicationConfig {

  @Bean
  public StartupGuard startupGuard(AppProperties properties) {
    return StartupGuard.verify(properties);
  }

  /** Options and consents, loaded once at start (AR-04). */
  @Bean
  public OptionCatalog optionCatalog(
      AppProperties properties, ResourceLoader resourceLoader, StartupGuard guard) {
    String location = properties.options().location();
    Resource resource = resourceLoader.getResource(location);
    if (!resource.exists()) {
      throw new InvalidOptionsException(location + ": file not found");
    }
    try {
      return new OptionCatalogLoader().load(resource.getInputStream(), location);
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read the options file " + location, e);
    }
  }

  @Bean
  public FormConfigService formConfigService(OptionCatalog catalog, AppProperties properties) {
    return new FormConfigService(
        catalog,
        new FormConfigService.FormSettings(
            properties.conferenceName(),
            properties.recaptcha().siteKey(),
            properties.recaptcha().testMode()));
  }
}
