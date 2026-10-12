package si.konferenca.registration.config;

import java.nio.file.Path;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.support.TransactionTemplate;
import si.konferenca.registration.application.CaptchaVerifier;
import si.konferenca.registration.application.Notifier;
import si.konferenca.registration.application.RegistrationCopyWriter;
import si.konferenca.registration.application.RegistrationService;
import si.konferenca.registration.application.RegistrationStore;
import si.konferenca.registration.domain.OptionCatalog;
import si.konferenca.registration.domain.RegistrationValidator;
import si.konferenca.registration.infrastructure.captcha.GoogleCaptchaVerifier;
import si.konferenca.registration.infrastructure.captcha.TestModeCaptchaVerifier;
import si.konferenca.registration.infrastructure.jsoncopy.FileRegistrationCopyWriter;
import si.konferenca.registration.infrastructure.mail.MailNotifier;

/** Wires the registration use case to its adapters (specification S2). */
@Configuration(proxyBeanMethods = false)
public class RegistrationConfiguration {

  @Bean
  RegistrationCopyWriter registrationCopyWriter(AppProperties properties) {
    return new FileRegistrationCopyWriter(Path.of(properties.jsonCopyDir()));
  }

  @Bean
  CaptchaVerifier captchaVerifier(AppProperties properties) {
    AppProperties.Recaptcha recaptcha = properties.recaptcha();
    return recaptcha.testMode()
        ? new TestModeCaptchaVerifier()
        : new GoogleCaptchaVerifier(recaptcha.verifyUrl(), recaptcha.secretKey());
  }

  @Bean
  RegistrationService registrationService(
      OptionCatalog catalog,
      CaptchaVerifier captcha,
      RegistrationStore store,
      RegistrationCopyWriter copies,
      Notifier notifier,
      TransactionTemplate transactions,
      Clock clock) {
    return new RegistrationService(
        new RegistrationValidator(catalog), captcha, store, copies, notifier, transactions, clock);
  }

  @Bean
  Notifier notifier(JavaMailSender mailSender, AppProperties properties) {
    return new MailNotifier(
        mailSender,
        properties.mail().from(),
        properties.conferenceName(),
        properties.organizer().emails().stream()
            .map(String::strip)
            .filter(e -> !e.isEmpty())
            .toList());
  }
}
