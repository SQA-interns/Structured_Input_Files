package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import si.konferenca.registration.RegistrationApplication;

/**
 * AC-001-13 on the production reCAPTCHA path (DoD-P05): test mode off, the verification URL points
 * to a local mock of Google's siteverify endpoint.
 */
@SpringBootTest(
    classes = RegistrationApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class US001CaptchaLiveModeAcceptanceTest {

  static final String SECRET = "mock-secret-for-acceptance";
  static final String ACCEPTED_TOKEN = "token-google-accepts";
  static final List<Map<String, String>> VERIFY_CALLS = new CopyOnWriteArrayList<>();
  static final HttpServer VERIFY_MOCK;
  static final Path OWN_COPY_DIR;

  static {
    try {
      OWN_COPY_DIR = Files.createTempDirectory("registration-copies-live-captcha");
      VERIFY_MOCK = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    VERIFY_MOCK.createContext(
        "/recaptcha/api/siteverify",
        exchange -> {
          String form =
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
          Map<String, String> fields = new java.util.HashMap<>();
          for (String pair : form.split("&")) {
            String[] kv = pair.split("=", 2);
            fields.put(
                URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
          }
          VERIFY_CALLS.add(fields);
          boolean ok = ACCEPTED_TOKEN.equals(fields.get("response"));
          byte[] body =
              ("{\"success\": "
                      + ok
                      + (ok ? "" : ", \"error-codes\": [\"invalid-input-response\"]")
                      + "}")
                  .getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, body.length);
          try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
          }
        });
    VERIFY_MOCK.start();
  }

  @LocalServerPort int port;

  @DynamicPropertySource
  static void configure(DynamicPropertyRegistry registry) {
    AcceptanceTestBase.registerDatabase(registry, AcceptanceTestBase.POSTGRES);
    AcceptanceTestBase.registerMail(
        registry,
        AcceptanceTestBase.MAILPIT.getHost(),
        AcceptanceTestBase.MAILPIT.getMappedPort(1025));
    registry.add("JSON_COPY_DIR", OWN_COPY_DIR::toString);
    registry.add("OPTIONS_LOCATION", () -> "classpath:acceptance/options.json");
    registry.add("ORGANIZER_USERNAME", () -> AcceptanceTestBase.ORGANIZER_USERNAME);
    registry.add("ORGANIZER_PASSWORD", () -> AcceptanceTestBase.ORGANIZER_PASSWORD);
    registry.add("ORGANIZER_EMAILS", () -> AcceptanceTestBase.ORGANIZER_EMAIL_1);
    registry.add("RECAPTCHA_TEST_MODE", () -> "false");
    registry.add("RECAPTCHA_SITE_KEY", () -> "mock-site-key");
    registry.add("RECAPTCHA_SECRET_KEY", () -> SECRET);
    registry.add(
        "RECAPTCHA_VERIFY_URL",
        () ->
            "http://127.0.0.1:" + VERIFY_MOCK.getAddress().getPort() + "/recaptcha/api/siteverify");
  }

  private Db db;

  @BeforeEach
  void reset() throws Exception {
    VERIFY_CALLS.clear();
    db =
        new Db(
            AcceptanceTestBase.POSTGRES.getJdbcUrl(),
            AcceptanceTestBase.POSTGRES.getUsername(),
            AcceptanceTestBase.POSTGRES.getPassword());
    db.deleteAllRegistrations();
    new CopyDir(OWN_COPY_DIR).reset();
  }

  @Test
  @DisplayName("AC-001-13 a token the live verification rejects is rejected")
  void ac001_13_tokenRejectedByLiveVerificationIsRejected() throws Exception {
    Map<String, Object> request = Registrations.validExternal();
    request.put("recaptchaToken", "token-google-rejects");

    Response response = new Api("http://localhost:" + port).postRegistration(request);

    assertThat(response.status()).as(response.toString()).isEqualTo(400);
    assertThat(response.errorFields()).contains("recaptchaToken");
    assertThat(VERIFY_CALLS).hasSize(1);
    assertThat(VERIFY_CALLS.get(0))
        .containsEntry("secret", SECRET)
        .containsEntry("response", "token-google-rejects");
    assertThat(db.countRegistrations()).isZero();
    assertThat(new CopyDir(OWN_COPY_DIR).files()).isEmpty();
  }

  @Test
  @DisplayName("AC-001-13 the test-mode token is rejected when test mode is off")
  void ac001_13_testModeTokenIsRejectedInLiveMode() throws Exception {
    Map<String, Object> request = Registrations.validExternal();

    Response response = new Api("http://localhost:" + port).postRegistration(request);

    assertThat(response.status()).as(response.toString()).isEqualTo(400);
    assertThat(response.errorFields()).contains("recaptchaToken");
    assertThat(db.countRegistrations()).isZero();
  }

  @Test
  @DisplayName("AC-001-03 a token the live verification accepts lets the registration through")
  void ac001_03_tokenAcceptedByLiveVerificationIsAccepted() throws Exception {
    Map<String, Object> request = Registrations.validExternal();
    request.put("recaptchaToken", ACCEPTED_TOKEN);

    Response response = new Api("http://localhost:" + port).postRegistration(request);

    assertThat(response.status()).as(response.toString()).isEqualTo(201);
    assertThat(VERIFY_CALLS).hasSize(1);
    assertThat(db.registration(response.json("$.id"))).isNotNull();
  }
}
