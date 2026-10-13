package si.konferenca.registration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import si.konferenca.registration.RegistrationProperties;

class SecuritySettingsTest {

  static RegistrationProperties properties(
      boolean testMode,
      String siteKey,
      String secretKey,
      String user,
      String password,
      boolean httpsOnly) {
    return new RegistrationProperties(
        "Conference",
        "classpath:conference.json",
        Path.of("copies"),
        new RegistrationProperties.Mail("from@example.com", List.of("o@example.com")),
        new RegistrationProperties.Recaptcha(
            testMode, siteKey, secretKey, URI.create("https://verify.example")),
        new RegistrationProperties.Organizer(user, password, httpsOnly),
        new RegistrationProperties.Cors(List.of()),
        new RegistrationProperties.RateLimit(1, 2, 3),
        16384);
  }

  @Test
  void productionGuardAcceptsASafeConfiguration() {
    ProductionStartGuard guard =
        new ProductionStartGuard(properties(false, "site", "secret", "u", "p", true));
    guard.afterPropertiesSet();
    assertThat(ProductionStartGuard.problems(properties(false, "site", "secret", "u", "p", true)))
        .isEmpty();
  }

  @Test
  void productionGuardListsEveryProblem() {
    assertThat(ProductionStartGuard.problems(properties(true, "", " ", "u", "p", false)))
        .containsExactly(
            "RECAPTCHA_TEST_MODE is on",
            "RECAPTCHA_SITE_KEY and RECAPTCHA_SECRET_KEY must be set",
            "ORGANIZER_HTTPS_ONLY is off");
    assertThat(ProductionStartGuard.problems(properties(false, null, "secret", "u", "p", true)))
        .containsExactly("RECAPTCHA_SITE_KEY and RECAPTCHA_SECRET_KEY must be set");
    assertThat(ProductionStartGuard.problems(properties(false, "site", null, "u", "p", true)))
        .containsExactly("RECAPTCHA_SITE_KEY and RECAPTCHA_SECRET_KEY must be set");
    assertThat(ProductionStartGuard.problems(properties(false, "site", "", "u", "p", true)))
        .containsExactly("RECAPTCHA_SITE_KEY and RECAPTCHA_SECRET_KEY must be set");
  }

  @Test
  void productionGuardRefusesToStart() {
    ProductionStartGuard guard =
        new ProductionStartGuard(properties(true, "site", "secret", "u", "p", true));
    assertThatThrownBy(guard::afterPropertiesSet)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Refusing to start in production: RECAPTCHA_TEST_MODE is on");
  }

  @Test
  void organizerAccountHoldsOnlyABcryptHash() {
    SecurityConfiguration configuration = new SecurityConfiguration();
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    UserDetails organizer =
        configuration
            .organizers(properties(false, "s", "s", "boss", "very-secret-password", true), encoder)
            .loadUserByUsername("boss");

    assertThat(organizer.getPassword()).startsWith("$2").doesNotContain("very-secret-password");
    assertThat(encoder.matches("very-secret-password", organizer.getPassword())).isTrue();
    assertThat(organizer.getAuthorities())
        .extracting(Object::toString)
        .containsExactly("ROLE_ORGANIZER");
    assertThat(configuration.passwordEncoder()).isInstanceOf(BCryptPasswordEncoder.class);
  }

  @Test
  void organizerAccountNeedsUsernameAndPassword() {
    SecurityConfiguration configuration = new SecurityConfiguration();
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    for (RegistrationProperties p :
        List.of(
            properties(false, "s", "s", "", "p", true),
            properties(false, "s", "s", "u", " ", true),
            properties(false, "s", "s", null, "p", true),
            properties(false, "s", "s", "u", null, true))) {
      assertThatThrownBy(() -> configuration.organizers(p, encoder))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("ORGANIZER_USERNAME and ORGANIZER_PASSWORD must be set");
    }
  }

  @Test
  void corsIsOffUnlessOriginsAreConfigured() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/form-config");
    assertThat(SecurityConfiguration.cors(List.of()).getCorsConfiguration(request)).isNull();

    CorsConfiguration config =
        SecurityConfiguration.cors(List.of("http://localhost:5173")).getCorsConfiguration(request);
    assertThat(config).isNotNull();
    assertThat(config.getAllowedOrigins()).containsExactly("http://localhost:5173");
    assertThat(config.getAllowedMethods()).containsExactly("GET", "POST");
    assertThat(config.getAllowedHeaders()).containsExactly("Content-Type", "Authorization");
    assertThat(
            SecurityConfiguration.cors(List.of("http://localhost:5173"))
                .getCorsConfiguration(new MockHttpServletRequest("GET", "/actuator/health")))
        .isNull();
  }
}
