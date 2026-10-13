package si.konferenca.registration.adapter.captcha;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The production reCAPTCHA path against a mocked siteverify endpoint, including a rejected token
 * (DoD-P05, SR-01).
 */
class GoogleCaptchaVerifierIntegrationTest {

  private HttpServer server;
  private final AtomicReference<String> requestBody = new AtomicReference<>();
  private volatile int status = 200;
  private volatile String answer = "{\"success\": true}";

  @BeforeEach
  void startStub() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/siteverify",
        exchange -> {
          requestBody.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          byte[] body = answer.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(status, body.length);
          try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
          }
        });
    server.start();
  }

  @AfterEach
  void stopStub() {
    server.stop(0);
  }

  private GoogleCaptchaVerifier verifier() {
    return new GoogleCaptchaVerifier(
        URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/siteverify"),
        "secret-key");
  }

  @Test
  void acceptedTokenIsVerifiedWithSecretTokenAndClientIp() {
    assertThat(verifier().verify("token value", "203.0.113.5")).isTrue();
    assertThat(requestBody.get())
        .isEqualTo("secret=secret-key&response=token+value&remoteip=203.0.113.5");
  }

  @Test
  void clientIpIsOmittedWhenUnknown() {
    assertThat(verifier().verify("t", null)).isTrue();
    assertThat(requestBody.get()).isEqualTo("secret=secret-key&response=t");
  }

  @Test
  void rejectedTokenIsRefused() {
    answer = "{\"success\": false, \"error-codes\": [\"invalid-input-response\"]}";
    assertThat(verifier().verify("bad", "203.0.113.5")).isFalse();
  }

  @Test
  void answersOtherThanABooleanSuccessAreRefused() {
    answer = "{\"success\": \"true\"}";
    assertThat(verifier().verify("t", null)).isFalse();
    answer = "{}";
    assertThat(verifier().verify("t", null)).isFalse();
    answer = "not json";
    assertThat(verifier().verify("t", null)).isFalse();
  }

  @Test
  void nonOkStatusIsRefused() {
    status = 500;
    assertThat(verifier().verify("t", null)).isFalse();
  }

  @Test
  void unreachableEndpointIsRefused() throws IOException {
    int closedPort;
    try (ServerSocket socket = new ServerSocket(0)) {
      closedPort = socket.getLocalPort();
    }
    GoogleCaptchaVerifier unreachable =
        new GoogleCaptchaVerifier(URI.create("http://127.0.0.1:" + closedPort + "/x"), "s");
    assertThat(unreachable.verify("t", null)).isFalse();
  }

  @Test
  void interruptedVerificationIsRefusedAndKeepsTheInterruptFlag() {
    Thread.currentThread().interrupt();
    try {
      assertThat(verifier().verify("t", null)).isFalse();
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  void testModeAcceptsOnlyThePassToken() {
    TestModeCaptchaVerifier testMode = new TestModeCaptchaVerifier();
    assertThat(testMode.verify("test-pass", null)).isTrue();
    assertThat(testMode.verify("test-pass ", null)).isFalse();
    assertThat(testMode.verify(null, null)).isFalse();
  }

  @Test
  void configurationChoosesTheVerifierByTestMode() {
    CaptchaConfiguration config = new CaptchaConfiguration();
    assertThat(config.captchaVerifier(true, "", URI.create("http://x")))
        .isInstanceOf(TestModeCaptchaVerifier.class);
    assertThat(config.captchaVerifier(false, "s", URI.create("http://x")))
        .isInstanceOf(GoogleCaptchaVerifier.class);
  }
}
