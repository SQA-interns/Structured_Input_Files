package si.konferenca.registration.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import si.konferenca.registration.RegistrationApplication;

/** HTTP-level controls of S3 and S10 on the running application (SR-03, SR-06, SB-10, ES-07). */
@SpringBootTest(
    classes = RegistrationApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HttpControlsIntegrationTest {

  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16.15-alpine");
  static final Path COPY_DIR;

  static {
    POSTGRES.start();
    try {
      COPY_DIR = Files.createTempDirectory("integration-copies");
    } catch (java.io.IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @DynamicPropertySource
  static void configure(DynamicPropertyRegistry r) {
    r.add("DB_URL", POSTGRES::getJdbcUrl);
    r.add("DB_USERNAME", POSTGRES::getUsername);
    r.add("POSTGRES_PASSWORD", POSTGRES::getPassword);
    r.add("MAIL_HOST", () -> "127.0.0.1");
    r.add("MAIL_PORT", () -> "1");
    r.add("JSON_COPY_DIR", COPY_DIR::toString);
    r.add("RECAPTCHA_TEST_MODE", () -> "true");
    r.add("ORGANIZER_USERNAME", () -> "organizer");
    r.add("ORGANIZER_PASSWORD", () -> "integration-only-password");
    r.add("ORGANIZER_EMAILS", () -> "org@test.local");
    r.add("RATE_LIMIT_REGISTRATIONS_PER_MINUTE", () -> "2");
    r.add("MAX_REQUEST_BYTES", () -> "512");
  }

  @LocalServerPort int port;
  private final HttpClient client = HttpClient.newHttpClient();

  private HttpResponse<String> send(HttpRequest.Builder builder) throws Exception {
    return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
  }

  private HttpRequest.Builder request(String path, String clientIp) {
    return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
        .header("X-Forwarded-For", clientIp);
  }

  private HttpRequest.Builder post(String body, String clientIp) {
    return request("/api/registrations", clientIp)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body));
  }

  @Test
  void apiResponsesCarryTheSecurityHeaders() throws Exception {
    HttpResponse<String> response = send(request("/api/options", "198.51.100.1").GET());

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.headers().firstValue("Content-Security-Policy"))
        .hasValue("default-src 'none'; frame-ancestors 'none'");
    assertThat(response.headers().firstValue("X-Content-Type-Options")).hasValue("nosniff");
    assertThat(response.headers().firstValue("X-Frame-Options")).hasValue("DENY");
    assertThat(response.headers().firstValue("Referrer-Policy")).hasValue("no-referrer");
  }

  @Test
  void settingsExposeNoSecret() throws Exception {
    HttpResponse<String> response = send(request("/api/settings", "198.51.100.2").GET());

    assertThat(response.body())
        .contains("\"testMode\":true")
        .contains("\"siteKey\":\"\"")
        .doesNotContain("integration-only-password");
  }

  @Test
  void bodiesAboveTheLimitAre413() throws Exception {
    HttpResponse<String> response =
        send(post("{\"firstName\":\"" + "x".repeat(600) + "\"}", "198.51.100.3"));

    assertThat(response.statusCode()).isEqualTo(413);
    assertThat(response.headers().firstValue("Content-Type"))
        .hasValueSatisfying(v -> assertThat(v).startsWith("application/problem+json"));
  }

  @Test
  void registrationsAboveTheRateLimitAre429PerClient() throws Exception {
    for (int i = 0; i < 2; i++) {
      assertThat(send(post("{}", "198.51.100.4")).statusCode()).isEqualTo(400);
    }
    HttpResponse<String> limited = send(post("{}", "198.51.100.4"));
    HttpResponse<String> otherClient = send(post("{}", "198.51.100.5"));

    assertThat(limited.statusCode()).isEqualTo(429);
    assertThat(limited.headers().firstValue("Retry-After")).isPresent();
    assertThat(otherClient.statusCode()).isEqualTo(400);
  }

  @Test
  void malformedJsonAndWrongContentTypeAndUnknownPathsGiveProblemsWithoutInternals()
      throws Exception {
    HttpResponse<String> malformed = send(post("{not json", "198.51.100.6"));
    HttpResponse<String> text =
        send(
            request("/api/registrations", "198.51.100.7")
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString("x")));
    HttpResponse<String> unknown = send(request("/api/nothing-here", "198.51.100.8").GET());

    assertThat(malformed.statusCode()).isEqualTo(400);
    assertThat(malformed.body()).contains("\"field\":\"body\"").doesNotContain("Exception");
    assertThat(text.statusCode()).isEqualTo(415);
    assertThat(unknown.statusCode()).isEqualTo(404);
    assertThat(unknown.body()).doesNotContain("Exception").doesNotContain("trace");
  }

  @Test
  void organizerOverPlainHttpFromAnotherHostIsRefusedBeforeAuthentication() throws Exception {
    HttpResponse<String> plain = send(request("/api/organizer/export", "203.0.113.9").GET());
    HttpResponse<String> forwardedHttps =
        send(
            request("/api/organizer/export", "203.0.113.10")
                .header("X-Forwarded-Proto", "https")
                .GET());

    assertThat(plain.statusCode()).isEqualTo(403);
    assertThat(plain.headers().firstValue("WWW-Authenticate")).isEmpty();
    assertThat(forwardedHttps.statusCode()).isEqualTo(401);
  }

  @Test
  void healthReportsLivenessAndReadiness() throws Exception {
    HttpResponse<String> readiness = send(request("/actuator/health/readiness", "127.0.0.1").GET());
    HttpResponse<String> liveness = send(request("/actuator/health/liveness", "127.0.0.1").GET());
    HttpResponse<String> env = send(request("/actuator/env", "127.0.0.1").GET());

    assertThat(readiness.statusCode()).isEqualTo(200);
    assertThat(readiness.body()).contains("UP");
    assertThat(liveness.statusCode()).isEqualTo(200);
    assertThat(env.statusCode()).isEqualTo(404);
  }
}
