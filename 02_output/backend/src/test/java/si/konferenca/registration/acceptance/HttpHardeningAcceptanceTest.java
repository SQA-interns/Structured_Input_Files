package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static si.konferenca.registration.acceptance.support.Payloads.external;
import static si.konferenca.registration.acceptance.support.Payloads.with;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.Api;

/** SR-03 size limit, SB-10 security headers, SB-07 errors without internals, NFR-04 health. */
class HttpHardeningAcceptanceTest extends AcceptanceTestBase {

  @Test
  @DisplayName("SR-03: a request body above the size limit is refused and nothing is stored")
  void sr_03_oversizedBodyIsRefused() {
    Map<String, Object> valid = external();
    Map<String, Object> huge = with(valid, "organization", "x".repeat(20_000));

    Api.Response response = api.register(huge);

    assertThat(response.status()).isEqualTo(413);
    assertNothingStored(JSON_COPY_DIR, valid);
  }

  @Test
  @DisplayName("SB-10: API responses carry security headers")
  void sb_10_securityHeadersArePresent() {
    Api.Response response = api.get("/api/form-config");

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.header("X-Content-Type-Options")).isEqualTo("nosniff");
    assertThat(response.header("X-Frame-Options")).isEqualTo("DENY");
    assertThat(response.header("Content-Security-Policy")).contains("frame-ancestors 'none'");
  }

  @Test
  @DisplayName("SB-07: error responses expose no internal details")
  void sb_07_errorsExposeNoInternals() {
    Api.Response malformed = api.postRaw("/api/registrations", "{\"type\": \"EXTERNAL\", ");
    Api.Response wrongType = api.postRaw("/api/registrations", "{\"type\": 42}");
    Api.Response unknown = api.get("/api/does-not-exist");

    assertThat(malformed.status()).isEqualTo(400);
    assertThat(wrongType.status()).isEqualTo(400);
    assertThat(unknown.status()).isBetween(400, 499);
    for (Api.Response r : new Api.Response[] {malformed, wrongType, unknown}) {
      assertThat(r.text())
          .doesNotContain("Exception")
          .doesNotContain("org.springframework")
          .doesNotContain("si.konferenca")
          .doesNotContain("tools.jackson")
          .doesNotContainIgnoringCase("trace");
    }
  }

  @Test
  @DisplayName("NFR-04: the backend reports liveness and readiness")
  void nfr_04_healthAndReadinessAreReported() {
    Api.Response readiness = api.get("/actuator/health/readiness");
    Api.Response liveness = api.get("/actuator/health/liveness");

    assertThat(readiness.status()).isEqualTo(200);
    assertThat(readiness.json().<String>read("$.status")).isEqualTo("UP");
    assertThat(liveness.status()).isEqualTo(200);
    assertThat(liveness.json().<String>read("$.status")).isEqualTo("UP");
  }
}
