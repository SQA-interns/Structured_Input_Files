package si.konferenca.registration.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Refuses unsafe configurations at startup (SR-02, environments.md). Messages name the setting,
 * never its value.
 */
final class StartupSafetyChecks {

  private static final Set<String> ENVIRONMENTS = Set.of("production", "local", "test");

  private StartupSafetyChecks() {}

  static void verify(AppProperties p) {
    List<String> problems = new ArrayList<>();
    if (!ENVIRONMENTS.contains(p.environment())) {
      problems.add("APP_ENVIRONMENT must be production, local or test");
    }
    boolean testMode = p.recaptcha().testMode();
    if (!testMode && (blank(p.recaptcha().siteKey()) || blank(p.recaptcha().secretKey()))) {
      problems.add("RECAPTCHA_SITE_KEY and RECAPTCHA_SECRET_KEY must be set");
    }
    if (p.isProduction()) {
      if (testMode) {
        problems.add("APP_RECAPTCHA_TEST_MODE must be off in production");
      }
      if (!p.organizer().httpsOnly()) {
        problems.add("APP_ORGANIZER_HTTPS_ONLY must be on in production");
      }
      if (!blank(p.cors().allowedOrigin())) {
        problems.add("APP_CORS_ALLOWED_ORIGIN is allowed only in local");
      }
      if (blank(p.conference().configFile())) {
        problems.add("APP_CONFERENCE_CONFIG_FILE must be set in production");
      }
    } else if (!blank(p.cors().allowedOrigin()) && !"local".equals(p.environment())) {
      problems.add("APP_CORS_ALLOWED_ORIGIN is allowed only in local");
    }
    if (!problems.isEmpty()) {
      throw new IllegalStateException("Unsafe configuration: " + String.join("; ", problems));
    }
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
