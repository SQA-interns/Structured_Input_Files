package si.konferenca.registration.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import si.konferenca.registration.domain.OptionCatalog;

/** Core beans: settings, the option catalog and the clock. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AppProperties.class)
public class CoreConfiguration {

  @Bean
  OptionCatalog optionCatalog(AppProperties properties, ResourceLoader resourceLoader) {
    return OptionCatalogLoader.load(resourceLoader.getResource(properties.optionsLocation()));
  }

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }
}
