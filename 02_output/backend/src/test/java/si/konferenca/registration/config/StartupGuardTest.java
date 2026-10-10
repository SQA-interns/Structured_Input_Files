package si.konferenca.registration.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class StartupGuardTest {

  private static AppProperties properties(
      String environment,
      String username,
      String password,
      List<String> emails,
      boolean httpsOnly,
      String options,
      boolean testMode,
      String siteKey,
      String secretKey,
      String cors) {
    return new AppProperties(
        environment,
        "Konf",
        new AppProperties.Mail("from@konf.si"),
        new AppProperties.Organizer(username, password, emails, httpsOnly),
        new AppProperties.Options(options),
        new AppProperties.JsonCopy("/data"),
        new AppProperties.Recaptcha(testMode, siteKey, secretKey, "https://verify"),
        new AppProperties.Cors(cors),
        new AppProperties.RateLimits(
            new AppProperties.Limit(10, Duration.ofMinutes(10)),
            new AppProperties.ExportLimit(20, Duration.ofMinutes(10)),
            new AppProperties.PublicLimit(120, Duration.ofMinutes(1))),
        new AppProperties.Request(16384));
  }

  private static AppProperties validProduction() {
    return properties(
        "production",
        "org",
        "a-password-of-16+",
        List.of("o@konf.si"),
        true,
        "file:/c.json",
        false,
        "site",
        "secret",
        null);
  }

  @Test
  void validProductionStarts() {
    assertThat(StartupGuard.problems(validProduction())).isEmpty();
    assertThat(StartupGuard.verify(validProduction())).isNotNull();
    assertThat(validProduction().production()).isTrue();
  }

  @Test
  void unsafeProductionIsRefused() {
    AppProperties unsafe =
        properties(
            "production",
            "org",
            "a-password-of-16+",
            List.of("o@konf.si"),
            false,
            AppProperties.DEFAULT_OPTIONS_LOCATION,
            true,
            "",
            " ",
            "http://localhost:5173");

    assertThat(StartupGuard.problems(unsafe))
        .containsExactly(
            "reCAPTCHA test mode must be off in production",
            "reCAPTCHA keys (RECAPTCHA_SITE_KEY, RECAPTCHA_SECRET_KEY) are not set",
            "app.options.location must point to the production options file",
            "organizer HTTPS-only access must be on in production",
            "CORS (app.cors.allowed-origin) is for local development only");
    assertThatThrownBy(() -> StartupGuard.verify(unsafe))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageStartingWith("Refusing to start: reCAPTCHA test mode");
  }

  @Test
  void eachMissingKeyAloneIsReported() {
    assertThat(
            StartupGuard.problems(
                properties(
                    "production",
                    "org",
                    "a-password-of-16+",
                    List.of("o@konf.si"),
                    true,
                    "file:/c.json",
                    false,
                    null,
                    "s",
                    null)))
        .hasSize(1);
    assertThat(
            StartupGuard.problems(
                properties(
                    "production",
                    "org",
                    "a-password-of-16+",
                    List.of("o@konf.si"),
                    true,
                    "file:/c.json",
                    false,
                    "k",
                    null,
                    null)))
        .hasSize(1);
  }

  @Test
  void localMayUseTestModeAndDefaults() {
    AppProperties local =
        properties(
            "local",
            "org",
            "a-password-of-16+",
            List.of("o@konf.si"),
            false,
            AppProperties.DEFAULT_OPTIONS_LOCATION,
            true,
            null,
            null,
            "http://localhost:5173");

    assertThat(StartupGuard.problems(local)).isEmpty();
    assertThat(local.production()).isFalse();
  }

  @Test
  void organizerSettingsAndEnvironmentAreRequired() {
    AppProperties missing =
        properties("staging", " ", null, List.of(" "), true, "x", false, "k", "s", null);
    AppProperties shortPassword =
        properties(
            "test", "org", "a".repeat(15), List.of("o@konf.si"), true, "x", false, "k", "s", null);

    assertThat(StartupGuard.problems(missing))
        .containsExactly(
            "app.environment must be production, local or test",
            "organizer username (ORGANIZER_USERNAME) is not set",
            "organizer password (ORGANIZER_PASSWORD) is not set",
            "organizer emails (ORGANIZER_EMAILS) are not set");
    assertThat(StartupGuard.problems(shortPassword))
        .containsExactly("organizer password (ORGANIZER_PASSWORD) is shorter than 16");
    assertThat(
            StartupGuard.problems(
                properties(
                    "test",
                    "org",
                    "a".repeat(16),
                    List.of("o@konf.si"),
                    true,
                    "x",
                    false,
                    "k",
                    "s",
                    null)))
        .isEmpty();
  }
}
