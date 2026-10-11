package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static si.konferenca.registration.acceptance.support.Payloads.external;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.Api;

/** SR-03, SB-06: every public /api endpoint is rate limited per client. */
class RateLimitAcceptanceTest extends AcceptanceTestBase {

  @DynamicPropertySource
  static void lowLimits(DynamicPropertyRegistry registry) {
    registry.add("app.rate-limit.registration-per-minute", () -> "3");
    registry.add("app.rate-limit.form-config-per-minute", () -> "5");
    registry.add("app.rate-limit.export-per-minute", () -> "2");
  }

  @Test
  @DisplayName("SR-03: form configuration is limited per client address")
  void sr_03_formConfigIsRateLimitedPerClient() {
    for (int i = 0; i < 5; i++) {
      assertThat(api.get("/api/form-config").status()).isEqualTo(200);
    }

    Api.Response limited = api.get("/api/form-config");
    Api.Response otherClient = api.get("/api/form-config", "X-Forwarded-For", "198.51.100.9");

    assertThat(limited.status()).isEqualTo(429);
    assertThat(limited.header("Retry-After")).isNotBlank();
    assertThat(otherClient.status()).isEqualTo(200);
  }

  @Test
  @DisplayName("SR-03: registration submissions are limited per client address")
  void sr_03_registrationIsRateLimited() {
    for (int i = 0; i < 3; i++) {
      assertThat(api.register(external()).status()).isEqualTo(201);
    }

    Api.Response limited = api.register(external());

    assertThat(limited.status()).isEqualTo(429);
  }

  @Test
  @DisplayName("SR-03: export requests, including failed logins, are limited per client address")
  void sr_03_exportIsRateLimited() {
    for (int i = 0; i < 2; i++) {
      assertThat(api.getAsOrganizer("/api/export", "organizer", "wrong-password").status())
          .isEqualTo(401);
    }

    Api.Response limited = api.getAsOrganizer("/api/export", "organizer", "wrong-password");

    assertThat(limited.status()).isEqualTo(429);
  }
}
