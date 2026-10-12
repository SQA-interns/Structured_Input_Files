package si.konferenca.registration.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class StartupGuardsTest {

  private static AppProperties properties(
      boolean testMode,
      String siteKey,
      String secret,
      String options,
      String user,
      List<String> emails) {
    return new AppProperties(
        "Conf",
        options,
        "/data",
        new AppProperties.Mail("from@x.si"),
        new AppProperties.Organizer(user, "secret-password", emails, true),
        new AppProperties.Recaptcha(testMode, siteKey, secret, "https://verify"),
        List.of(),
        new AppProperties.RateLimit(10, 10, 10),
        16384);
  }

  private static void check(AppProperties p, String... profiles) {
    MockEnvironment env = new MockEnvironment();
    env.setActiveProfiles(profiles);
    new StartupGuards(p, env).afterPropertiesSet();
  }

  @Test
  void acceptsTestModeLocallyAndLiveKeysInProduction() {
    assertThatCode(
            () -> check(properties(true, "", "", "classpath:x.json", "org", List.of("a@b.si"))))
        .doesNotThrowAnyException();
    assertThatCode(
            () ->
                check(
                    properties(false, "site", "secret", "file:/x.json", "org", List.of("a@b.si")),
                    "prod"))
        .doesNotThrowAnyException();
  }

  @Test
  void refusesTestModeInProduction() {
    assertThatThrownBy(
            () -> check(properties(true, "s", "s", "file:/x", "org", List.of("a@b.si")), "prod"))
        .hasMessageContaining("RECAPTCHA_TEST_MODE");
  }

  @Test
  void refusesEmptyKeysWithoutTestMode() {
    assertThatThrownBy(() -> check(properties(false, "", "s", "file:/x", "org", List.of("a@b.si"))))
        .hasMessageContaining("RECAPTCHA_SITE_KEY");
    assertThatThrownBy(
            () -> check(properties(false, "s", " ", "file:/x", "org", List.of("a@b.si"))))
        .hasMessageContaining("RECAPTCHA_SECRET_KEY");
  }

  @Test
  void refusesTheBundledOptionsInProduction() {
    assertThatThrownBy(
            () ->
                check(
                    properties(false, "s", "s", "classpath:x.json", "org", List.of("a@b.si")),
                    "prod"))
        .hasMessageContaining("OPTIONS_LOCATION");
  }

  @Test
  void refusesMissingOrganizerSettings() {
    assertThatThrownBy(() -> check(properties(true, "", "", "classpath:x", " ", List.of("a@b.si"))))
        .hasMessageContaining("ORGANIZER_USERNAME");
    assertThatThrownBy(() -> check(properties(true, "", "", "classpath:x", "org", List.of(" "))))
        .hasMessageContaining("ORGANIZER_EMAILS");
  }
}
