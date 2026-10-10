package si.konferenca.registration.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CaptchaVerifierTest {

  private HttpServer server;
  private final AtomicReference<String> requestBody = new AtomicReference<>();
  private volatile int status = 200;
  private volatile String answer = "{\"success\": true}";

  @BeforeEach
  void start() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/verify",
        exchange -> {
          requestBody.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          byte[] bytes = answer.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(status, bytes.length);
          try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
          }
        });
    server.start();
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  private GoogleRecaptchaVerifier verifier() {
    return new GoogleRecaptchaVerifier(
        "http://127.0.0.1:" + server.getAddress().getPort() + "/verify", "s&cret");
  }

  @Test
  void postsSecretTokenAndClientAddress() {
    assertThat(verifier().verify("tok en", "203.0.113.5")).isTrue();
    assertThat(requestBody.get()).isEqualTo("secret=s%26cret&response=tok+en&remoteip=203.0.113.5");
  }

  @Test
  void omitsMissingClientAddress() {
    assertThat(verifier().verify("t", null)).isTrue();
    assertThat(requestBody.get()).doesNotContain("remoteip");
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "200|{\"success\": false}",
        "200|{\"success\": \"true\"}",
        "200|{}",
        "200|null",
        "200|not json",
        "500|{\"success\": true}"
      })
  void rejectsEverythingButSuccessTrue(int code, String body) {
    status = code;
    answer = body;

    assertThat(verifier().verify("t", "1.2.3.4")).isFalse();
  }

  @Test
  void unreachableEndpointRejects() {
    GoogleRecaptchaVerifier verifier =
        new GoogleRecaptchaVerifier("http://127.0.0.1:1/verify", "s");

    assertThat(verifier.verify("t", "1.2.3.4")).isFalse();
  }

  @Test
  void testModeAcceptsOnlyItsToken() {
    TestModeCaptchaVerifier verifier = new TestModeCaptchaVerifier();

    assertThat(verifier.verify("test-pass", null)).isTrue();
    assertThat(verifier.verify("other", null)).isFalse();
    assertThat(verifier.verify(null, null)).isFalse();
  }
}
