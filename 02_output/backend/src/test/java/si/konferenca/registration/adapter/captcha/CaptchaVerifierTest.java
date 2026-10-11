package si.konferenca.registration.adapter.captcha;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.domain.CaptchaUnavailableException;

class CaptchaVerifierTest {

  private HttpServer server;
  private final AtomicReference<String> lastBody = new AtomicReference<>();

  private String serve(int status, String body) throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/verify",
        ex -> {
          lastBody.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
          ex.sendResponseHeaders(status, bytes.length);
          try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
          }
        });
    server.start();
    return "http://127.0.0.1:" + server.getAddress().getPort() + "/verify";
  }

  @AfterEach
  void stop() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void testModeAcceptsOnlyTheTestToken() {
    TestModeCaptchaVerifier v = new TestModeCaptchaVerifier();

    assertThat(v.verify("TEST-PASS", null)).isTrue();
    assertThat(v.verify("test-pass", null)).isFalse();
    assertThat(v.verify(null, null)).isFalse();
  }

  @Test
  void acceptsSuccessAndSendsEncodedForm() throws Exception {
    RecaptchaHttpVerifier v =
        new RecaptchaHttpVerifier(serve(200, "{\"success\": true}"), "se cret&=");

    assertThat(v.verify("tok+en", "10.0.0.1")).isTrue();
    assertThat(lastBody.get())
        .isEqualTo("secret=se+cret%26%3D&response=tok%2Ben&remoteip=10.0.0.1");
  }

  @Test
  void omitsRemoteIpWhenUnknown() throws Exception {
    RecaptchaHttpVerifier v = new RecaptchaHttpVerifier(serve(200, "{\"success\": true}"), "s");

    v.verify("t", null);

    assertThat(lastBody.get()).isEqualTo("secret=s&response=t");
  }

  @Test
  void rejectsWhenTheVerifierSaysNoOrOmitsSuccess() throws Exception {
    assertThat(new RecaptchaHttpVerifier(serve(200, "{\"success\": false}"), "s").verify("t", null))
        .isFalse();
    stop();
    assertThat(new RecaptchaHttpVerifier(serve(200, "{}"), "s").verify("t", null)).isFalse();
  }

  @Test
  void unavailableOnErrorStatusBadBodyOrNoServer() throws Exception {
    assertThatThrownBy(() -> new RecaptchaHttpVerifier(serve(500, "{}"), "s").verify("t", null))
        .isInstanceOf(CaptchaUnavailableException.class);
    stop();
    assertThatThrownBy(() -> new RecaptchaHttpVerifier(serve(200, "<html>"), "s").verify("t", null))
        .isInstanceOf(CaptchaUnavailableException.class);
    stop();
    server = null;
    assertThatThrownBy(
            () -> new RecaptchaHttpVerifier("http://127.0.0.1:1/verify", "s").verify("t", null))
        .isInstanceOf(CaptchaUnavailableException.class);
  }
}
