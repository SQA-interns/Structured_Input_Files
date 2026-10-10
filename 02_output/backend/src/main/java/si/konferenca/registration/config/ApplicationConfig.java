package si.konferenca.registration.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.javamail.JavaMailSender;
import si.konferenca.registration.application.CaptchaVerifier;
import si.konferenca.registration.application.FormConfigService;
import si.konferenca.registration.application.JsonCopyStore;
import si.konferenca.registration.application.Notifier;
import si.konferenca.registration.application.RegistrationRecorder;
import si.konferenca.registration.application.RegistrationService;
import si.konferenca.registration.application.RegistrationStore;
import si.konferenca.registration.domain.OptionCatalog;
import si.konferenca.registration.domain.RegistrationValidator;
import si.konferenca.registration.infrastructure.GoogleRecaptchaVerifier;
import si.konferenca.registration.infrastructure.InvalidOptionsException;
import si.konferenca.registration.infrastructure.MailNotifier;
import si.konferenca.registration.infrastructure.OptionCatalogLoader;
import si.konferenca.registration.infrastructure.TestModeCaptchaVerifier;
import si.konferenca.registration.persistence.FileJsonCopyStore;

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

  @Bean
  public JsonCopyStore jsonCopyStore(AppProperties properties) {
    return new FileJsonCopyStore(Path.of(properties.jsonCopy().directory()));
  }

  @Bean
  public RegistrationRecorder registrationRecorder(RegistrationStore store, JsonCopyStore copies) {
    return new RegistrationRecorder(store, copies);
  }

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }

  /** Test mode only when configured (SR-02); otherwise the production verification. */
  @Bean
  public CaptchaVerifier captchaVerifier(AppProperties properties) {
    AppProperties.Recaptcha recaptcha = properties.recaptcha();
    if (recaptcha.testMode()) {
      return new TestModeCaptchaVerifier();
    }
    return new GoogleRecaptchaVerifier(
        recaptcha.verifyUrl(), recaptcha.secretKey() == null ? "" : recaptcha.secretKey());
  }

  @Bean
  public RegistrationService registrationService(
      OptionCatalog catalog,
      CaptchaVerifier captcha,
      RegistrationStore store,
      RegistrationRecorder recorder,
      Clock clock,
      Notifier notifier) {
    return new RegistrationService(
        new RegistrationValidator(catalog), captcha, store, recorder, clock, notifier);
  }

  @Bean
  public Notifier notifier(JavaMailSender mailSender, AppProperties properties) {
    return new MailNotifier(
        mailSender,
        new MailNotifier.MailSettings(
            properties.mail().sender(),
            properties.conferenceName(),
            properties.organizer().emails()));
  }
}
