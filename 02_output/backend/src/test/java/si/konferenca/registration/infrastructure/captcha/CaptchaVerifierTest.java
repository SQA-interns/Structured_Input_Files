package si.konferenca.registration.infrastructure.captcha;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CaptchaVerifierTest {

  private HttpServer server;
  private final List<String> requests = new ArrayList<>();
  private volatile int status = 200;
  private volatile String answer = "{\"success\": true}";

  @BeforeEach
  void start() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/verify",
        exchange -> {
          requests.add(
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
  void stop() {
    server.stop(0);
  }

  private GoogleCaptchaVerifier verifier() {
    return new GoogleCaptchaVerifier(
        "http://127.0.0.1:" + server.getAddress().getPort() + "/verify", "s3cret");
  }

  @Test
  void testModeAcceptsOnlyTheTestToken() {
    TestModeCaptchaVerifier verifier = new TestModeCaptchaVerifier();

    assertThat(verifier.verify("test-mode-pass", null)).isTrue();
    assertThat(verifier.verify("other", null)).isFalse();
    assertThat(verifier.verify(null, null)).isFalse();
  }

  @Test
  void liveModePostsSecretTokenAndIpAndAcceptsSuccess() {
    assertThat(verifier().verify("tok en", "10.0.0.1")).isTrue();
    assertThat(requests)
        .singleElement()
        .satisfies(
            r ->
                assertThat(r)
                    .contains("secret=s3cret")
                    .contains("response=tok+en")
                    .contains("remoteip=10.0.0.1"));
  }

  @Test
  void liveModeOmitsAMissingIp() {
    assertThat(verifier().verify("t", null)).isTrue();
    assertThat(requests.get(0)).doesNotContain("remoteip");
  }

  @Test
  void liveModeRejectsFailureAndMalformedAnswersAndErrors() {
    answer = "{\"success\": false}";
    assertThat(verifier().verify("t", "ip")).isFalse();
    answer = "{\"success\": \"true\"}";
    assertThat(verifier().verify("t", "ip")).isFalse();
    answer = "{}";
    assertThat(verifier().verify("t", "ip")).isFalse();
    answer = "not json";
    assertThat(verifier().verify("t", "ip")).isFalse();
    answer = "{\"success\": true}";
    status = 500;
    assertThat(verifier().verify("t", "ip")).isFalse();
  }

  @Test
  void liveModeGivesUpAfterTheReadTimeout() {
    server.createContext(
        "/slow",
        exchange -> {
          try {
            Thread.sleep(7000);
            exchange.sendResponseHeaders(200, -1);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        });
    GoogleCaptchaVerifier slow =
        new GoogleCaptchaVerifier(
            "http://127.0.0.1:" + server.getAddress().getPort() + "/slow", "s");

    long start = System.nanoTime();
    assertThat(slow.verify("t", "ip")).isFalse();
    assertThat(java.time.Duration.ofNanos(System.nanoTime() - start))
        .isLessThan(java.time.Duration.ofSeconds(6));
  }

  @Test
  void liveModeRejectsWhenTheServiceIsUnreachable() {
    GoogleCaptchaVerifier unreachable = new GoogleCaptchaVerifier("http://127.0.0.1:1/verify", "s");

    assertThat(unreachable.verify("t", "ip")).isFalse();
  }
}
