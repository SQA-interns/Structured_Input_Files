package si.konferenca.registration.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.config.AppProperties;

class RecaptchaVerifierTest {

  private HttpServer server;
  private final AtomicInteger status = new AtomicInteger(200);
  private final AtomicReference<String> answer = new AtomicReference<>("{\"success\":true}");
  private final List<String> forms = new CopyOnWriteArrayList<>();
  private String url;

  @BeforeEach
  void startStub() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/verify",
        exchange -> {
          forms.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          byte[] body = answer.get().getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(status.get(), body.length);
          try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
          }
        });
    server.start();
    url = "http://127.0.0.1:" + server.getAddress().getPort() + "/verify";
  }

  @AfterEach
  void stopStub() {
    server.stop(0);
  }

  @Test
  void acceptsWhenTheEndpointReportsSuccess() {
    GoogleRecaptchaVerifier verifier = new GoogleRecaptchaVerifier(url, "s&cret");

    assertThat(verifier.verify("tok en", "203.0.113.5")).isTrue();
    assertThat(forms).containsExactly("secret=s%26cret&response=tok+en&remoteip=203.0.113.5");
  }

  @Test
  void omitsAMissingClientAddress() {
    new GoogleRecaptchaVerifier(url, "secret").verify("token", null);
    new GoogleRecaptchaVerifier(url, "secret").verify("token", " ");

    assertThat(forms)
        .containsExactly("secret=secret&response=token", "secret=secret&response=token");
  }

  @Test
  void rejectsWithoutCallingForBlankTokens() {
    GoogleRecaptchaVerifier verifier = new GoogleRecaptchaVerifier(url, "secret");

    assertThat(verifier.verify(null, "1.2.3.4")).isFalse();
    assertThat(verifier.verify("  ", "1.2.3.4")).isFalse();
    assertThat(forms).isEmpty();
  }

  @Test
  void rejectsUnsuccessfulAnswers() {
    GoogleRecaptchaVerifier verifier = new GoogleRecaptchaVerifier(url, "secret");

    for (String body :
        List.of(
            "{\"success\":false}", "{\"success\":\"true\"}", "{}", "not json", "null", "[true]")) {
      answer.set(body);
      assertThat(verifier.verify("token", null)).as(body).isFalse();
    }
  }

  @Test
  void rejectsNonOkStatus() {
    status.set(500);

    assertThat(new GoogleRecaptchaVerifier(url, "secret").verify("token", null)).isFalse();
  }

  @Test
  void rejectsWhenTheEndpointIsUnreachable() {
    server.stop(0);

    assertThat(new GoogleRecaptchaVerifier(url, "secret").verify("token", null)).isFalse();
  }

  @Test
  void rejectsAndKeepsTheInterruptFlagWhenInterrupted() throws Exception {
    HttpClient client = mock(HttpClient.class);
    when(client.send(any(), any(HttpResponse.BodyHandler.class)))
        .thenThrow(new InterruptedException());

    boolean result = new GoogleRecaptchaVerifier(url, "secret", client).verify("token", null);

    assertThat(result).isFalse();
    assertThat(Thread.interrupted()).isTrue();
  }

  @Test
  void testModeAcceptsOnlyTheTestToken() {
    TestModeRecaptchaVerifier verifier = new TestModeRecaptchaVerifier();

    assertThat(verifier.verify("test-pass", null)).isTrue();
    assertThat(verifier.verify("test-pass ", null)).isFalse();
    assertThat(verifier.verify("other", null)).isFalse();
    assertThat(verifier.verify(null, null)).isFalse();
  }

  @Test
  void configurationChoosesTheVerifierByTestMode() {
    RecaptchaConfiguration configuration = new RecaptchaConfiguration();

    assertThat(configuration.recaptchaVerifier(properties(true)))
        .isInstanceOf(TestModeRecaptchaVerifier.class);
    assertThat(configuration.recaptchaVerifier(properties(false)))
        .isInstanceOf(GoogleRecaptchaVerifier.class);
  }

  private AppProperties properties(boolean testMode) {
    return new AppProperties(
        "test",
        new AppProperties.Conference("classpath:conference.json", "Conf"),
        new AppProperties.JsonCopy(java.nio.file.Path.of("copies")),
        new AppProperties.Recaptcha(testMode, "site", "secret", url),
        new AppProperties.Organizer("o", "p", List.of("o@example.org"), true),
        new AppProperties.Mail("from@example.org"),
        new AppProperties.Limits(1, 1, 1, 100),
        new AppProperties.Cors(""));
  }
}
