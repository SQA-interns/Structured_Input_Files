package si.konferenca.registration.infrastructure.captcha;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** DoD-P05: the production reCAPTCHA code path against a mocked verification endpoint. */
class RecaptchaVerifierTest {

  HttpServer server;
  final List<String> requestBodies = new ArrayList<>();
  final List<String> contentTypes = new ArrayList<>();
  volatile int status = 200;
  volatile String reply = "{\"success\": true}";
  volatile long delayMillis = 0;

  @BeforeEach
  void startMockEndpoint() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/recaptcha/api/siteverify",
        exchange -> {
          requestBodies.add(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          contentTypes.add(exchange.getRequestHeaders().getFirst("Content-Type"));
          try {
            Thread.sleep(delayMillis);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          byte[] body = reply.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(status, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.start();
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  RecaptchaVerifier verifier() {
    return new RecaptchaVerifier(
        URI.create(
            "http://127.0.0.1:" + server.getAddress().getPort() + "/recaptcha/api/siteverify"),
        "secret key&=",
        Duration.ofMillis(500));
  }

  @Test
  void SR_01_acceptedTokenIsVerifiedWithSecretAndResponse() {
    assertThat(verifier().verify("token-from-widget")).isTrue();

    assertThat(requestBodies).hasSize(1);
    String body = URLDecoder.decode(requestBodies.get(0), StandardCharsets.UTF_8);
    assertThat(body).contains("secret=secret key&=", "response=token-from-widget");
    assertThat(requestBodies.get(0)).contains("secret=secret+key%26%3D");
    assertThat(contentTypes.get(0)).startsWith("application/x-www-form-urlencoded");
  }

  @Test
  void DoD_P05_rejectedTokenIsRejected() {
    reply = "{\"success\": false, \"error-codes\": [\"invalid-input-response\"]}";

    assertThat(verifier().verify("forged")).isFalse();
  }

  @Test
  void SR_01_failingOrMalformedVerificationRejects() {
    status = 500;
    assertThat(verifier().verify("token")).isFalse();

    status = 200;
    reply = "not json";
    assertThat(verifier().verify("token")).isFalse();

    reply = "{\"success\": \"yes\"}";
    assertThat(verifier().verify("token")).isFalse();
  }

  @Test
  void SR_01_slowEndpointTimesOutAndRejects() {
    delayMillis = 2000;

    assertThat(verifier().verify("token")).isFalse();
  }

  @Test
  void SR_01_unreachableEndpointRejects() {
    RecaptchaVerifier unreachable =
        new RecaptchaVerifier(URI.create("http://127.0.0.1:1/x"), "s", Duration.ofMillis(500));

    assertThat(unreachable.verify("token")).isFalse();
  }

  @Test
  void SR_01_blankTokenIsRejectedWithoutACall() {
    assertThat(verifier().verify(null)).isFalse();
    assertThat(verifier().verify(" ")).isFalse();
    assertThat(requestBodies).isEmpty();
  }
}
