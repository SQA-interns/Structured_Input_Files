package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import si.konferenca.registration.acceptance.support.AcIdDisplayNames;
import si.konferenca.registration.acceptance.support.AcceptanceSupport;
import si.konferenca.registration.acceptance.support.TestEnvironment;

/** AC-001-13 and AC-008-04: rate limits and request size limit (SR-03), with small limits. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayNameGeneration(AcIdDisplayNames.class)
class RateLimitAcceptanceTest extends AcceptanceSupport {

  private static final int LIMIT = 3;

  @LocalServerPort private int port;

  @DynamicPropertySource
  static void environment(DynamicPropertyRegistry registry) {
    Map<String, Supplier<Object>> limits =
        Map.of(
            "app.rate-limit.registration.max-requests", () -> LIMIT,
            "app.rate-limit.registration.window", () -> "PT10M",
            "app.rate-limit.export.max-requests", () -> LIMIT,
            "app.rate-limit.export.window", () -> "PT10M");
    TestEnvironment.register(registry, limits);
  }

  @Override
  protected int port() {
    return port;
  }

  @Test
  void ac_001_13_registrationsAboveTheLimitPerClientAreRefused() {
    String client = "203.0.113.10";
    for (int i = 0; i < LIMIT; i++) {
      Map<String, Object> invalid = validExternal(uniqueEmail("ac-001-13-invalid"));
      invalid.put("firstName", "");
      assertThat(register(invalid, client).statusCode()).isEqualTo(400);
    }
    String email = uniqueEmail("ac-001-13-limited");

    HttpResponse<String> limited = register(validExternal(email), client);

    assertThat(limited.statusCode()).isEqualTo(429);
    assertThat(limited.headers().firstValue("Retry-After")).isPresent();
    assertThat(json(limited).path("error").asString()).isEqualTo("TOO_MANY_REQUESTS");
    assertThat(registrationCountByEmail(email)).isZero();
  }

  @Test
  void ac_001_13_limitIsCountedPerClient() {
    String busy = "203.0.113.11";
    for (int i = 0; i < LIMIT; i++) {
      Map<String, Object> invalid = validExternal(uniqueEmail("ac-001-13-busy"));
      invalid.put("lastName", "");
      register(invalid, busy);
    }
    String email = uniqueEmail("ac-001-13-other");

    HttpResponse<String> other = register(validExternal(email), "203.0.113.12");

    assertThat(other.statusCode()).as(other.body()).isEqualTo(201);
    assertThat(registrationCountByEmail(email)).isEqualTo(1);
  }

  @Test
  void ac_001_13_bodyAboveTheSizeLimitIsRefused() {
    String email = uniqueEmail("ac-001-13-size");
    Map<String, Object> body = validExternal(email);
    body.put("organization", "x".repeat(20_000));
    long before = registrationCount();

    HttpResponse<String> response = register(body, "203.0.113.13");

    assertThat(response.statusCode()).isEqualTo(413);
    assertThat(json(response).path("error").asString()).isEqualTo("PAYLOAD_TOO_LARGE");
    assertThat(registrationCount()).isEqualTo(before);
  }

  @Test
  void ac_008_04_exportsAboveTheLimitPerClientAreRefused() {
    String client = "203.0.113.20";
    for (int i = 0; i < LIMIT; i++) {
      assertThat(export(organizerHeaders(client)).statusCode()).isEqualTo(200);
    }

    HttpResponse<byte[]> limited = export(organizerHeaders(client));

    assertThat(limited.statusCode()).isEqualTo(429);
    assertThat(limited.headers().firstValue("Content-Type").orElse("")).doesNotContain(XLSX);
  }
}
