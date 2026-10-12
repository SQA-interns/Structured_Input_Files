package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.TestStack;

/**
 * AC-001-07, AC-002-06 and DoD-P05: the production reCAPTCHA code path (test mode off) against a
 * local stub of the Google verification endpoint (docs/02_contracts/recaptcha.json).
 */
class RecaptchaProductionVerifierAcceptanceTest extends AcceptanceTestBase {

  private static final String SECRET = "stub-secret-key-for-tests";
  private static final String ACCEPTED_TOKEN = "token-google-accepts";
  private static final List<Map<String, String>> VERIFY_CALLS = new CopyOnWriteArrayList<>();
  private static final HttpServer STUB = startStub();

  @DynamicPropertySource
  static void settings(DynamicPropertyRegistry registry) {
    TestStack.register(
        registry,
        Map.of(
            "APP_ENVIRONMENT", "production",
            "RECAPTCHA_TEST_MODE", "false",
            "RECAPTCHA_SITE_KEY", "stub-site-key-for-tests",
            "RECAPTCHA_SECRET_KEY", SECRET,
            "RECAPTCHA_VERIFY_URL",
                "http://127.0.0.1:" + STUB.getAddress().getPort() + "/recaptcha/api/siteverify"));
  }

  @BeforeEach
  void resetCalls() {
    VERIFY_CALLS.clear();
  }

  @Test
  @DisplayName("AC-001-07 external: token accepted by the verification endpoint is accepted")
  void ac_001_07_tokenAcceptedByVerifierIsAccepted() throws Exception {
    Map<String, Object> body = validExternal();
    body.put("recaptchaToken", ACCEPTED_TOKEN);

    register(body);

    assertThat(VERIFY_CALLS).hasSize(1);
    assertThat(VERIFY_CALLS.getFirst())
        .containsEntry("secret", SECRET)
        .containsEntry("response", ACCEPTED_TOKEN);
  }

  @Test
  @DisplayName("AC-001-07 external: token rejected by the verification endpoint is rejected")
  void ac_001_07_tokenRejectedByVerifierIsRejected() throws Exception {
    Map<String, Object> body = validExternal();
    body.put("recaptchaToken", "token-google-rejects");

    MvcResult result = submit(body);

    assertThat(result.getResponse().getStatus()).as(body(result)).isEqualTo(400);
    assertThat(VERIFY_CALLS).hasSize(1);
    assertNothingChanged();
  }

  @Test
  @DisplayName("AC-001-07 external: the test-mode token is not accepted in production mode")
  void ac_001_07_testModeTokenIsRejectedInProduction() throws Exception {
    Map<String, Object> body = validExternal();
    body.put("recaptchaToken", TestStack.TEST_MODE_TOKEN);

    MvcResult result = submit(body);

    assertThat(result.getResponse().getStatus()).as(body(result)).isEqualTo(400);
    assertNothingChanged();
  }

  @Test
  @DisplayName("AC-002-06 student: token rejected by the verification endpoint is rejected")
  void ac_002_06_studentTokenRejectedByVerifierIsRejected() throws Exception {
    Map<String, Object> body = validStudent();
    body.put("recaptchaToken", "token-google-rejects");

    MvcResult result = submit(body);

    assertThat(result.getResponse().getStatus()).as(body(result)).isEqualTo(400);
    assertNothingChanged();
  }

  @Test
  @DisplayName("AC-002-06 student: token accepted by the verification endpoint is accepted")
  void ac_002_06_studentTokenAcceptedByVerifierIsAccepted() throws Exception {
    Map<String, Object> body = validStudent();
    body.put("recaptchaToken", ACCEPTED_TOKEN);

    register(body);

    assertThat(VERIFY_CALLS).hasSize(1);
  }

  private static HttpServer startStub() {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext(
          "/recaptcha/api/siteverify",
          exchange -> {
            String form =
                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> fields = parseForm(form);
            VERIFY_CALLS.add(fields);
            boolean success =
                "POST".equals(exchange.getRequestMethod())
                    && SECRET.equals(fields.get("secret"))
                    && ACCEPTED_TOKEN.equals(fields.get("response"));
            byte[] answer =
                (success
                        ? "{\"success\": true, \"hostname\": \"localhost\"}"
                        : "{\"success\": false, \"error-codes\": [\"invalid-input-response\"]}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, answer.length);
            try (OutputStream out = exchange.getResponseBody()) {
              out.write(answer);
            }
          });
      server.start();
      return server;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static Map<String, String> parseForm(String form) {
    Map<String, String> fields = new java.util.HashMap<>();
    for (String pair : form.split("&")) {
      int eq = pair.indexOf('=');
      if (eq > 0) {
        fields.put(
            URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
            URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
      }
    }
    return fields;
  }
}
