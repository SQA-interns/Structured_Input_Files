package si.konferenca.registration.config;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import si.konferenca.registration.adapter.captcha.RecaptchaHttpVerifier;
import si.konferenca.registration.adapter.captcha.TestModeCaptchaVerifier;
import si.konferenca.registration.adapter.catalog.JsonFileConferenceCatalog;
import si.konferenca.registration.adapter.export.ExcelRegistrationExporter;
import si.konferenca.registration.adapter.jsoncopy.FileJsonCopyStore;
import si.konferenca.registration.adapter.mail.MailRegistrationNotifier;
import si.konferenca.registration.domain.CaptchaVerifier;
import si.konferenca.registration.domain.ConferenceCatalog;
import si.konferenca.registration.domain.ExportService;
import si.konferenca.registration.domain.FormSettings;
import si.konferenca.registration.domain.JsonCopyStore;
import si.konferenca.registration.domain.RegistrationNotifier;
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
  CaptchaVerifier captchaVerifier(AppProperties props) {
    StartupSafetyChecks.verify(props);
    AppProperties.Recaptcha r = props.recaptcha();
    return r.testMode()
        ? new TestModeCaptchaVerifier()
        : new RecaptchaHttpVerifier(r.verifyUrl(), r.secretKey());
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
      JsonCopyStore jsonCopies,
      RegistrationNotifier notifier) {
    return new RegistrationService(
        new RegistrationValidator(catalog),
        captcha,
        store,
        jsonCopies,
        notifier,
        Clock.systemUTC());
  }

  @Bean
  ExportService exportService(RegistrationStore store) {
    return new ExportService(store, new ExcelRegistrationExporter());
  }

  @Bean(destroyMethod = "close")
  MailRegistrationNotifier registrationNotifier(JavaMailSender sender, AppProperties props) {
    return new MailRegistrationNotifier(
        sender,
        props.mail().from(),
        props.conference().name(),
        organizerAddresses(props.organizer().emails()));
  }

  /** Parses the comma-separated organizer list; an empty or invalid list stops the start. */
  static List<InternetAddress> organizerAddresses(String emails) {
    List<InternetAddress> result = new ArrayList<>();
    for (String part : (emails == null ? "" : emails).split(",")) {
      if (!part.isBlank()) {
        try {
          result.add(new InternetAddress(part.strip(), true));
        } catch (AddressException e) {
          throw new IllegalStateException("ORGANIZER_EMAILS contains an invalid address", e);
        }
      }
    }
    if (result.isEmpty()) {
      throw new IllegalStateException("ORGANIZER_EMAILS must list at least one address");
    }
    return result;
  }
}
