package si.konferenca.registration.captcha;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.RegistrationProperties;

class RecaptchaVerifierTest {

  private HttpServer server;
  private final AtomicReference<String> lastForm = new AtomicReference<>();
  private final AtomicReference<String> lastContentType = new AtomicReference<>();
  private volatile int status = 200;
  private volatile String answer = "{\"success\": true}";

  @BeforeEach
  void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/verify",
        exchange -> {
          lastForm.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          lastContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
          byte[] body = answer.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(status, body.length);
          try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
          }
        });
    server.start();
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
  }

  private RecaptchaVerifier verifier(boolean testMode, String secret, String url) {
    return new RecaptchaVerifier(
        new RegistrationProperties(
            "C",
            "classpath:conference.json",
            Path.of("x"),
            new RegistrationProperties.Mail("f@example.com", List.of()),
            new RegistrationProperties.Recaptcha(testMode, "site", secret, URI.create(url)),
            new RegistrationProperties.Organizer("u", "p", true),
            new RegistrationProperties.Cors(List.of()),
            new RegistrationProperties.RateLimit(1, 1, 1),
            100));
  }

  private String url() {
    return "http://127.0.0.1:" + server.getAddress().getPort() + "/verify";
  }

  @Test
  void testModeAcceptsOnlyTheTestTokenWithoutNetwork() {
    RecaptchaVerifier verifier = verifier(true, "secret", url());

    assertThat(verifier.verify(RecaptchaVerifier.TEST_MODE_TOKEN, "1.2.3.4")).isTrue();
    assertThat(verifier.verify("test-mode-token ", "1.2.3.4")).isFalse();
    assertThat(verifier.verify("anything", null)).isFalse();
    assertThat(lastForm.get()).isNull();
  }

  @Test
  void productionPathPostsSecretTokenAndClientAddress() {
    assertThat(verifier(false, "s&cret", url()).verify("tok en", "1.2.3.4")).isTrue();

    assertThat(lastForm.get()).isEqualTo("secret=s%26cret&response=tok+en&remoteip=1.2.3.4");
    assertThat(lastContentType.get()).isEqualTo("application/x-www-form-urlencoded");
  }

  @Test
  void clientAddressIsOptionalAndNullSecretIsSentEmpty() {
    assertThat(verifier(false, null, url()).verify("t", null)).isTrue();
    assertThat(lastForm.get()).isEqualTo("secret=&response=t");
  }

  @Test
  void rejectedUnlessHttp200WithSuccessTrue() {
    RecaptchaVerifier verifier = verifier(false, "secret", url());
    answer = "{\"success\": false}";
    assertThat(verifier.verify("t", null)).isFalse();
    answer = "{}";
    assertThat(verifier.verify("t", null)).isFalse();
    answer = "{\"success\": \"true\"}";
    assertThat(verifier.verify("t", null)).isFalse();
    answer = "not json";
    assertThat(verifier.verify("t", null)).isFalse();
    answer = "{\"success\": true}";
    status = 500;
    assertThat(verifier.verify("t", null)).isFalse();
  }

  @Test
  void unreachableVerifierRejects() {
    server.stop(0);
    assertThat(verifier(false, "secret", url()).verify("t", null)).isFalse();
  }

  @Test
  void interruptedVerificationRejectsAndKeepsTheInterruptFlag() {
    Thread.currentThread().interrupt();
    try {
      assertThat(verifier(false, "secret", url()).verify("t", null)).isFalse();
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
    } finally {
      Thread.interrupted();
    }
  }
}
