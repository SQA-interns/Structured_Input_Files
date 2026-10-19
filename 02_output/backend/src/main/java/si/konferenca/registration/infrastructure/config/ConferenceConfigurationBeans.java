package si.konferenca.registration.infrastructure.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import si.konferenca.registration.application.CaptchaSettings;
import si.konferenca.registration.application.FormConfigurationService;
import si.konferenca.registration.domain.ConferenceConfiguration;

/** Loads the conference configuration once at startup; an invalid file stops the start. */
@Configuration
public class ConferenceConfigurationBeans {

  @Bean
  ConferenceConfiguration conferenceConfiguration(
      @Value("${app.conference.config-file}") String configFile) {
    return new ConferenceConfigurationLoader().load(Path.of(configFile));
  }

  @Bean
  CaptchaSettings captchaSettings(
      @Value("${app.recaptcha.site-key}") String siteKey,
      @Value("${app.recaptcha.test-mode}") boolean testMode) {
    return new CaptchaSettings(testMode ? "" : siteKey, testMode);
  }

  @Bean
  FormConfigurationService formConfigurationService(
      ConferenceConfiguration configuration, CaptchaSettings captcha) {
    return new FormConfigurationService(configuration, captcha);
  }
}
