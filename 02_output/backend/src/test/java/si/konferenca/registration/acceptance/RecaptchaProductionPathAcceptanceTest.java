package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.Payloads;
import si.konferenca.registration.acceptance.support.TestInfrastructure;

/**
 * The production reCAPTCHA path (test mode off) against a mocked siteverify endpoint: AC-001-07,
 * AC-002-06, DoD-P05.
 */
class RecaptchaProductionPathAcceptanceTest extends AcceptanceTestBase {

  private static final String SECRET = "acceptance-recaptcha-secret";
  private static final String GOOD_TOKEN = "token-google-accepts";
  private static final List<Map<String, String>> VERIFY_CALLS = new CopyOnWriteArrayList<>();
  private static final HttpServer SITEVERIFY = startSiteverify();

  private static HttpServer startSiteverify() {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext(
          "/recaptcha/api/siteverify",
          exchange -> {
            String form =
                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> params = parseForm(form);
            VERIFY_CALLS.add(params);
            boolean ok =
                "POST".equals(exchange.getRequestMethod())
                    && SECRET.equals(params.get("secret"))
                    && GOOD_TOKEN.equals(params.get("response"));
            byte[] answer =
                (ok
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
    Map<String, String> params = new java.util.HashMap<>();
    for (String pair : form.split("&")) {
      int eq = pair.indexOf('=');
      if (eq > 0) {
        params.put(
            URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
            URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
      }
    }
    return params;
  }

  @DynamicPropertySource
  static void productionRecaptchaProperties(DynamicPropertyRegistry registry) {
    TestInfrastructure.register(
        registry,
        Map.of(
            "RECAPTCHA_TEST_MODE",
            "false",
            "RECAPTCHA_SITE_KEY",
            "acceptance-site-key",
            "RECAPTCHA_SECRET_KEY",
            SECRET,
            "RECAPTCHA_VERIFY_URL",
            "http://127.0.0.1:" + SITEVERIFY.getAddress().getPort() + "/recaptcha/api/siteverify"));
  }

  @Test
  void AC_001_07_tokenRejectedByVerifierIsRejectedAndNothingStored() {
    String email = Payloads.uniqueEmail("ac00107p");
    Map<String, Object> body = Payloads.external(email);
    body.put("recaptchaToken", "token-google-rejects");

    HttpResponse<String> response = postRegistration(body);

    assertRejectedWithFieldError(response, "recaptchaToken");
    assertNothingStoredOrSent(email);
    assertThat(VERIFY_CALLS)
        .anySatisfy(call -> assertThat(call).containsEntry("response", "token-google-rejects"));
  }

  @Test
  void AC_001_07_testModeTokenIsNotAcceptedWhenTestModeIsOff() {
    String email = Payloads.uniqueEmail("ac00107t");
    Map<String, Object> body = Payloads.external(email);
    body.put("recaptchaToken", TestInfrastructure.TEST_MODE_TOKEN);

    HttpResponse<String> response = postRegistration(body);

    assertRejectedWithFieldError(response, "recaptchaToken");
    assertNothingStoredOrSent(email);
  }

  @Test
  void AC_001_07_tokenAcceptedByVerifierIsAcceptedWithSecretSent() {
    String email = Payloads.uniqueEmail("ac00107g");
    Map<String, Object> body = Payloads.external(email);
    body.put("recaptchaToken", GOOD_TOKEN);

    HttpResponse<String> response = postRegistration(body);

    assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
    assertThat(registrationsWithEmail(email)).isEqualTo(1);
    assertThat(VERIFY_CALLS)
        .anySatisfy(
            call ->
                assertThat(call)
                    .containsEntry("secret", SECRET)
                    .containsEntry("response", GOOD_TOKEN));
  }

  @Test
  void AC_002_06_studentTokenRejectedByVerifierIsRejectedAndNothingStored() {
    String email = Payloads.uniqueEmail("ac00206p");
    Map<String, Object> body = Payloads.student(email);
    body.put("recaptchaToken", "token-google-rejects");

    HttpResponse<String> response = postRegistration(body);

    assertRejectedWithFieldError(response, "recaptchaToken");
    assertNothingStoredOrSent(email);
  }

  @Test
  void AC_002_06_studentTokenAcceptedByVerifierIsAccepted() {
    String email = Payloads.uniqueEmail("ac00206g");
    Map<String, Object> body = Payloads.student(email);
    body.put("recaptchaToken", GOOD_TOKEN);

    HttpResponse<String> response = postRegistration(body);

    assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
    assertThat(registrationsWithEmail(email)).isEqualTo(1);
  }
}
