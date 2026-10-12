package si.konferenca.registration.config;

import java.util.Arrays;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Refuses to start with unsafe or incomplete configuration (SR-02, ES-01, S5, S9). */
@Component
class StartupGuards implements InitializingBean {

  private final AppProperties properties;
  private final Environment environment;

  StartupGuards(AppProperties properties, Environment environment) {
    this.properties = properties;
    this.environment = environment;
  }

  @Override
  public void afterPropertiesSet() {
    boolean production = Arrays.asList(environment.getActiveProfiles()).contains("prod");
    AppProperties.Recaptcha recaptcha = properties.recaptcha();
    if (production && recaptcha.testMode()) {
      fail("RECAPTCHA_TEST_MODE must be off in production");
    }
    if (!recaptcha.testMode() && (blank(recaptcha.siteKey()) || blank(recaptcha.secretKey()))) {
      fail("RECAPTCHA_SITE_KEY and RECAPTCHA_SECRET_KEY are required when test mode is off");
    }
    if (production && !properties.optionsLocation().startsWith("file:")) {
      fail("OPTIONS_LOCATION must name a file in production");
    }
    AppProperties.Organizer organizer = properties.organizer();
    if (blank(organizer.username()) || blank(organizer.password())) {
      fail("ORGANIZER_USERNAME and ORGANIZER_PASSWORD are required");
    }
    if (organizer.emails() == null || organizer.emails().stream().allMatch(StartupGuards::blank)) {
      fail("ORGANIZER_EMAILS is required");
    }
    if (blank(properties.jsonCopyDir())) {
      fail("JSON_COPY_DIR is required");
    }
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private static void fail(String message) {
    throw new IllegalStateException("Invalid configuration: " + message);
  }
}
