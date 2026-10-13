package si.konferenca.registration.security;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import si.konferenca.registration.RegistrationProperties;

/**
 * Production refuses to start with reCAPTCHA test mode, empty keys or plain-HTTP export (SR-02).
 */
@Component
@Profile("prod")
public class ProductionStartGuard implements InitializingBean {

  private final RegistrationProperties properties;

  public ProductionStartGuard(RegistrationProperties properties) {
    this.properties = properties;
  }

  @Override
  public void afterPropertiesSet() {
    List<String> problems = problems(properties);
    if (!problems.isEmpty()) {
      throw new IllegalStateException(
          "Refusing to start in production: " + String.join("; ", problems));
    }
  }

  static List<String> problems(RegistrationProperties properties) {
    List<String> problems = new ArrayList<>();
    RegistrationProperties.Recaptcha recaptcha = properties.recaptcha();
    if (recaptcha.testMode()) {
      problems.add("RECAPTCHA_TEST_MODE is on");
    }
    if (isBlank(recaptcha.siteKey()) || isBlank(recaptcha.secretKey())) {
      problems.add("RECAPTCHA_SITE_KEY and RECAPTCHA_SECRET_KEY must be set");
    }
    if (!properties.organizer().httpsOnly()) {
      problems.add("ORGANIZER_HTTPS_ONLY is off");
    }
    return problems;
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
