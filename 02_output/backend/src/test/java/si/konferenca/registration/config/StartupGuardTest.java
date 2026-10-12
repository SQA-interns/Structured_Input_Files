package si.konferenca.registration.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StartupGuardTest {

  private static AppProperties properties(
      String environment,
      boolean testMode,
      String siteKey,
      String secretKey,
      String username,
      String password,
      List<String> emails) {
    return new AppProperties(
        environment,
        new AppProperties.Conference("classpath:conference.json", "Conf"),
        new AppProperties.JsonCopy(Path.of("copies")),
        new AppProperties.Recaptcha(testMode, siteKey, secretKey, "https://verify"),
        new AppProperties.Organizer(username, password, emails, true),
        new AppProperties.Mail("from@example.org"),
        new AppProperties.Limits(1, 1, 1, 100),
        new AppProperties.Cors(""));
  }

  private static AppProperties production() {
    return properties("production", false, "site", "secret", "org", "pw", List.of("o@example.org"));
  }

  @Test
  void acceptsCompleteProductionSettings() {
    StartupGuard guard = new StartupGuard(production());

    assertThatCode(guard::afterPropertiesSet).doesNotThrowAnyException();
  }

  @ParameterizedTest
  @ValueSource(strings = {"local", "test"})
  void allowsTestModeInLocalAndTest(String environment) {
    assertThatCode(
            () ->
                StartupGuard.check(
                    properties(environment, true, "", "", "org", "pw", List.of("o@example.org"))))
        .doesNotThrowAnyException();
  }

  @ParameterizedTest
  @ValueSource(strings = {"production", "staging", "LOCAL"})
  void refusesTestModeElsewhere(String environment) {
    AppProperties settings =
        properties(environment, true, "site", "secret", "org", "pw", List.of("o@example.org"));

    assertThatThrownBy(() -> new StartupGuard(settings).afterPropertiesSet())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("test mode");
  }

  @Test
  void refusesMissingKeysWithoutTestMode() {
    assertThatThrownBy(
            () ->
                StartupGuard.check(
                    properties("production", false, "", "s", "o", "p", List.of("o@example.org"))))
        .hasMessageContaining("site key");
    assertThatThrownBy(
            () ->
                StartupGuard.check(
                    properties("production", false, "s", " ", "o", "p", List.of("o@example.org"))))
        .hasMessageContaining("secret key");
    assertThatThrownBy(
            () ->
                StartupGuard.check(
                    properties("production", false, null, "s", "o", "p", List.of("o@example.org"))))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void refusesMissingOrganizerCredentials() {
    assertThatThrownBy(
            () ->
                StartupGuard.check(
                    properties("test", true, "", "", " ", "pw", List.of("o@example.org"))))
        .hasMessageContaining("Organizer");
    assertThatThrownBy(
            () ->
                StartupGuard.check(
                    properties("test", true, "", "", "org", null, List.of("o@example.org"))))
        .hasMessageContaining("Organizer");
  }

  @Test
  void refusesMissingOrInvalidOrganizerEmails() {
    assertThatThrownBy(() -> StartupGuard.check(properties("test", true, "", "", "o", "p", null)))
        .hasMessageContaining("ORGANIZER_EMAILS");
    assertThatThrownBy(
            () -> StartupGuard.check(properties("test", true, "", "", "o", "p", List.of())))
        .hasMessageContaining("ORGANIZER_EMAILS");
    assertThatThrownBy(
            () ->
                StartupGuard.check(
                    properties("test", true, "", "", "o", "p", List.of("a@b.org", "not-an-email"))))
        .hasMessageContaining("ORGANIZER_EMAILS");
    assertThatCode(
            () ->
                StartupGuard.check(
                    properties("test", true, "", "", "o", "p", List.of(" a@b.org ", "c@d.org"))))
        .doesNotThrowAnyException();
  }

  @Test
  void organizerToStringHidesThePassword() {
    AppProperties.Organizer organizer =
        new AppProperties.Organizer("org", "very-secret", List.of("a@b.org"), true);

    assertThat(organizer.toString()).doesNotContain("very-secret").contains("org");
  }
}
