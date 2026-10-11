package si.konferenca.registration.config;

import java.nio.file.Path;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import si.konferenca.registration.adapter.captcha.TestModeCaptchaVerifier;
import si.konferenca.registration.adapter.catalog.JsonFileConferenceCatalog;
import si.konferenca.registration.adapter.jsoncopy.FileJsonCopyStore;
import si.konferenca.registration.domain.CaptchaVerifier;
import si.konferenca.registration.domain.ConferenceCatalog;
import si.konferenca.registration.domain.FormSettings;
import si.konferenca.registration.domain.JsonCopyStore;
import si.konferenca.registration.domain.RegistrationService;
import si.konferenca.registration.domain.RegistrationStore;
import si.konferenca.registration.domain.RegistrationValidator;

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

  @Bean
  CaptchaVerifier captchaVerifier() {
    return new TestModeCaptchaVerifier();
  }

  @Bean
  JsonCopyStore jsonCopyStore(AppProperties props) {
    return new FileJsonCopyStore(props.jsonCopyDir());
  }

  @Bean
  RegistrationService registrationService(
      ConferenceCatalog catalog,
      CaptchaVerifier captcha,
      RegistrationStore store,
      JsonCopyStore jsonCopies) {
    return new RegistrationService(
        new RegistrationValidator(catalog), captcha, store, jsonCopies, Clock.systemUTC());
  }
}
