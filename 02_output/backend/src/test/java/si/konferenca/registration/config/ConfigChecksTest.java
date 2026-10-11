package si.konferenca.registration.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class ConfigChecksTest {

  private static AppProperties props(
      String env,
      boolean testMode,
      String site,
      String secret,
      boolean httpsOnly,
      String cors,
      String file) {
    return new AppProperties(
        env,
        new AppProperties.Conference("K", file),
        Path.of("data"),
        new AppProperties.Recaptcha(testMode, site, secret, "https://verify"),
        new AppProperties.Organizer("org", "0123456789abcdef", "o@example.si", httpsOnly),
        new AppProperties.Cors(cors),
        new AppProperties.RateLimit(1, 1, 1),
        16384,
        new AppProperties.Mail("from@example.si", false, false));
  }

  @Test
  void productionNeedsKeysHttpsNoCorsAndAConfigurationFile() {
    assertThatCode(
            () ->
                StartupSafetyChecks.verify(
                    props("production", false, "s", "k", true, "", "/c.json")))
        .doesNotThrowAnyException();
    assertThatThrownBy(
            () ->
                StartupSafetyChecks.verify(
                    props("production", true, "s", "k", false, "http://x", " ")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("APP_RECAPTCHA_TEST_MODE")
        .hasMessageContaining("APP_ORGANIZER_HTTPS_ONLY")
        .hasMessageContaining("APP_CORS_ALLOWED_ORIGIN")
        .hasMessageContaining("APP_CONFERENCE_CONFIG_FILE")
        .hasMessageNotContaining("http://x");
    assertThatThrownBy(
            () ->
                StartupSafetyChecks.verify(
                    props("production", false, "", "k", true, null, "/c.json")))
        .hasMessageContaining("RECAPTCHA_SITE_KEY");
    assertThatThrownBy(
            () ->
                StartupSafetyChecks.verify(
                    props("production", false, "s", null, true, null, "/c.json")))
        .hasMessageContaining("RECAPTCHA_SECRET_KEY");
  }

  @Test
  void localAndTestAllowTestModeAndOnlyLocalAllowsCors() {
    assertThatCode(
            () -> StartupSafetyChecks.verify(props("local", true, "", "", false, "http://x", null)))
        .doesNotThrowAnyException();
    assertThatCode(
            () -> StartupSafetyChecks.verify(props("test", true, null, null, false, " ", null)))
        .doesNotThrowAnyException();
    assertThatThrownBy(
            () -> StartupSafetyChecks.verify(props("test", true, "", "", true, "http://x", null)))
        .hasMessageContaining("APP_CORS_ALLOWED_ORIGIN");
    assertThatThrownBy(
            () -> StartupSafetyChecks.verify(props("test", false, "", "", true, null, null)))
        .hasMessageContaining("RECAPTCHA_SITE_KEY");
    assertThatThrownBy(
            () -> StartupSafetyChecks.verify(props("staging", true, "", "", true, null, null)))
        .hasMessageContaining("APP_ENVIRONMENT");
    assertThat(props("production", false, "", "", true, null, null).isProduction()).isTrue();
    assertThat(props("local", false, "", "", true, null, null).isProduction()).isFalse();
  }

  @Test
  void organizerAddressesMustBeValidAndPresent() {
    assertThat(ApplicationConfig.organizerAddresses(" a@example.si, ,b@example.si "))
        .extracting(Object::toString)
        .containsExactly("a@example.si", "b@example.si");
    assertThatThrownBy(() -> ApplicationConfig.organizerAddresses(" , "))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> ApplicationConfig.organizerAddresses(null))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> ApplicationConfig.organizerAddresses("not an address"))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void organizerPasswordIsStoredOnlyAsBcryptHashAndMustBeLongEnough() {
    SecurityConfig config = new SecurityConfig();
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    AppProperties ok = props("test", true, "", "", true, null, null);

    UserDetails user = config.organizer(ok, encoder).loadUserByUsername("org");

    assertThat(user.getPassword()).startsWith("$2").doesNotContain("0123456789abcdef");
    assertThat(encoder.matches("0123456789abcdef", user.getPassword())).isTrue();
    assertThat(user.getAuthorities())
        .extracting(Object::toString)
        .containsExactly("ROLE_ORGANIZER");

    AppProperties shortPassword =
        new AppProperties(
            "test",
            ok.conference(),
            ok.jsonCopyDir(),
            ok.recaptcha(),
            new AppProperties.Organizer("org", "0123456789abcde", "o@example.si", true),
            ok.cors(),
            ok.rateLimit(),
            1,
            ok.mail());
    assertThatThrownBy(() -> config.organizer(shortPassword, encoder))
        .hasMessageContaining("ORGANIZER_PASSWORD");
    AppProperties noUser =
        new AppProperties(
            "test",
            ok.conference(),
            ok.jsonCopyDir(),
            ok.recaptcha(),
            new AppProperties.Organizer(" ", "0123456789abcdef", "o@example.si", true),
            ok.cors(),
            ok.rateLimit(),
            1,
            ok.mail());
    assertThatThrownBy(() -> config.organizer(noUser, encoder))
        .hasMessageContaining("ORGANIZER_USERNAME");
  }
}
