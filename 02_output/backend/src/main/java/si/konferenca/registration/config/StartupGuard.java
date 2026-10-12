package si.konferenca.registration.config;

import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * Refuses to start with unsafe or incomplete settings (SR-02, ES-01): reCAPTCHA test mode outside
 * local and test, missing reCAPTCHA keys, missing organizer settings.
 */
@Component
public class StartupGuard implements InitializingBean {

  private static final Set<String> TEST_MODE_ENVIRONMENTS = Set.of("local", "test");
  private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

  private final AppProperties properties;

  public StartupGuard(AppProperties properties) {
    this.properties = properties;
  }

  /** Checks the settings once, when the application context is created. */
  @Override
  public void afterPropertiesSet() {
    check(properties);
  }

  static void check(AppProperties properties) {
    AppProperties.Recaptcha recaptcha = properties.recaptcha();
    String environment = properties.environment();
    if (recaptcha.testMode() && !TEST_MODE_ENVIRONMENTS.contains(environment)) {
      throw new IllegalStateException(
          "reCAPTCHA test mode is only allowed in the local and test environments");
    }
    if (!recaptcha.testMode() && (isBlank(recaptcha.siteKey()) || isBlank(recaptcha.secretKey()))) {
      throw new IllegalStateException(
          "reCAPTCHA site key and secret key are required when test mode is off");
    }
    AppProperties.Organizer organizer = properties.organizer();
    if (isBlank(organizer.username()) || isBlank(organizer.password())) {
      throw new IllegalStateException("Organizer username and password are required");
    }
    if (organizer.emails() == null
        || organizer.emails().isEmpty()
        || organizer.emails().stream().anyMatch(e -> !EMAIL.matcher(e.trim()).matches())) {
      throw new IllegalStateException("ORGANIZER_EMAILS must list valid email addresses");
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
