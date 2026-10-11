package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static si.konferenca.registration.acceptance.support.Payloads.external;
import static si.konferenca.registration.acceptance.support.Payloads.with;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.Api;

/**
 * SR-01, DoD-P05: the production reCAPTCHA code path (test mode off) against a mocked verification
 * endpoint (contract recaptcha.md).
 */
class RecaptchaProductionPathAcceptanceTest extends AcceptanceTestBase {

  private static final String SECRET = "mock-recaptcha-secret";
  private static final List<Map<String, String>> VERIFY_REQUESTS = new CopyOnWriteArrayList<>();
  private static final HttpServer VERIFIER = startVerifier();

  @DynamicPropertySource
  static void productionCaptcha(DynamicPropertyRegistry registry) {
    registry.add("app.recaptcha.test-mode", () -> "false");
    registry.add("app.recaptcha.site-key", () -> "mock-recaptcha-site-key");
    registry.add("app.recaptcha.secret-key", () -> SECRET);
    registry.add(
        "app.recaptcha.verify-url",
        () -> "http://127.0.0.1:" + VERIFIER.getAddress().getPort() + "/recaptcha/api/siteverify");
  }

  /** Answers success for "good-token", HTTP 500 for "error-token", success=false otherwise. */
  private static HttpServer startVerifier() {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext(
          "/recaptcha/api/siteverify",
          exchange -> {
            String body =
                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> form = parseForm(body);
            VERIFY_REQUESTS.add(form);
            String token = form.getOrDefault("response", "");
            int status = "error-token".equals(token) ? 500 : 200;
            String answer =
                "good-token".equals(token) && SECRET.equals(form.get("secret"))
                    ? "{\"success\": true, \"hostname\": \"localhost\"}"
                    : "{\"success\": false, \"error-codes\": [\"invalid-input-response\"]}";
            byte[] bytes = answer.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
              out.write(bytes);
            }
          });
      server.start();
      return server;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static Map<String, String> parseForm(String body) {
    Map<String, String> form = new HashMap<>();
    for (String pair : body.split("&")) {
      int eq = pair.indexOf('=');
      if (eq > 0) {
        form.put(
            URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
            URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
      }
    }
    return form;
  }

  @Test
  @DisplayName("SR-01/DoD-P05: a token the verifier accepts registers; secret and token are sent")
  void sr_01_acceptedTokenRegisters() {
    Map<String, Object> submission = with(external(), "captchaToken", "good-token");

    UUID id = registerAccepted(submission);

    assertThat(db.registration(id)).isPresent();
    assertThat(VERIFY_REQUESTS)
        .anySatisfy(
            form -> {
              assertThat(form.get("secret")).isEqualTo(SECRET);
              assertThat(form.get("response")).isEqualTo("good-token");
            });
  }

  @Test
  @DisplayName("AC-001-07/DoD-P05: a token the verifier rejects is refused and nothing is stored")
  void ac_001_07_rejectedTokenIsRefused() {
    Map<String, Object> valid = external();

    Api.Response response = api.register(with(valid, "captchaToken", "bad-token"));

    assertRejected(response, "captchaToken", JSON_COPY_DIR, valid);
  }

  @Test
  @DisplayName("SR-02/DoD-P05: the test token is not accepted when test mode is off")
  void sr_02_testTokenNotAcceptedInProductionMode() {
    Map<String, Object> valid = external();

    Api.Response response = api.register(valid);

    assertRejected(response, "captchaToken", JSON_COPY_DIR, valid);
  }

  @Test
  @DisplayName("SR-01/DoD-P05: an unavailable verifier yields 503 and nothing is stored")
  void sr_01_unavailableVerifierYieldsServiceUnavailable() {
    Map<String, Object> valid = external();

    Api.Response response = api.register(with(valid, "captchaToken", "error-token"));

    assertThat(response.status()).as(response.text()).isEqualTo(503);
    assertNothingStored(JSON_COPY_DIR, valid);
  }
}
