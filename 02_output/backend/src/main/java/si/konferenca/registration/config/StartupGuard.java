package si.konferenca.registration.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Refuses to start with missing secrets or unsafe production settings (ES-01, SR-02, SR-06). Names
 * the setting, never its value.
 */
public final class StartupGuard {

  static final int MIN_PASSWORD_LENGTH = 16;

  private StartupGuard(AppProperties properties) {
    List<String> problems = problems(properties);
    if (!problems.isEmpty()) {
      throw new IllegalStateException("Refusing to start: " + String.join("; ", problems));
    }
  }

  public static StartupGuard verify(AppProperties properties) {
    return new StartupGuard(properties);
  }

  static List<String> problems(AppProperties p) {
    List<String> problems = new ArrayList<>();
    if (!List.of("production", "local", "test").contains(p.environment())) {
      problems.add("app.environment must be production, local or test");
    }
    AppProperties.Organizer organizer = p.organizer();
    if (blank(organizer.username())) {
      problems.add("organizer username (ORGANIZER_USERNAME) is not set");
    }
    if (blank(organizer.password())) {
      problems.add("organizer password (ORGANIZER_PASSWORD) is not set");
    } else if (organizer.password().length() < MIN_PASSWORD_LENGTH) {
      problems.add(
          "organizer password (ORGANIZER_PASSWORD) is shorter than " + MIN_PASSWORD_LENGTH);
    }
    if (organizer.emails().stream().allMatch(StartupGuard::blank)) {
      problems.add("organizer emails (ORGANIZER_EMAILS) are not set");
    }
    if (p.production()) {
      AppProperties.Recaptcha recaptcha = p.recaptcha();
      if (recaptcha.testMode()) {
        problems.add("reCAPTCHA test mode must be off in production");
      }
      if (blank(recaptcha.siteKey()) || blank(recaptcha.secretKey())) {
        problems.add("reCAPTCHA keys (RECAPTCHA_SITE_KEY, RECAPTCHA_SECRET_KEY) are not set");
      }
      if (AppProperties.DEFAULT_OPTIONS_LOCATION.equals(p.options().location())) {
        problems.add("app.options.location must point to the production options file");
      }
      if (!organizer.httpsOnly()) {
        problems.add("organizer HTTPS-only access must be on in production");
      }
      if (!blank(p.cors().allowedOrigin())) {
        problems.add("CORS (app.cors.allowed-origin) is for local development only");
      }
    }
    return problems;
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
