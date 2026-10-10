package si.konferenca.registration.acceptance.support;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Mocked reCAPTCHA siteverify endpoint (contract recaptcha-siteverify.openapi.yaml). Accepts only
 * the token {@value #VALID_TOKEN} sent with the secret {@value #SECRET}.
 */
public final class CaptchaMock {

  public static final String VALID_TOKEN = "valid-token";
  public static final String REJECTED_TOKEN = "rejected-token";
  public static final String SECRET = "test-secret-key";

  private final HttpServer server;
  private final List<Map<String, String>> requests = new CopyOnWriteArrayList<>();

  private CaptchaMock(HttpServer server) {
    this.server = server;
  }

  public static CaptchaMock start() {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      CaptchaMock mock = new CaptchaMock(server);
      server.createContext(
          "/siteverify",
          exchange -> {
            String body =
                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> form = parseForm(body);
            mock.requests.add(form);
            boolean ok =
                "POST".equals(exchange.getRequestMethod())
                    && SECRET.equals(form.get("secret"))
                    && VALID_TOKEN.equals(form.get("response"));
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
      return mock;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static Map<String, String> parseForm(String body) {
    Map<String, String> form = new HashMap<>();
    for (String pair : body.split("&")) {
      int eq = pair.indexOf('=');
      if (eq > 0) {
        form.put(
            URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
            URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
      }
    }
    return form;
  }

  public String url() {
    return "http://127.0.0.1:" + server.getAddress().getPort() + "/siteverify";
  }

  /** Form parameters of every verification request received so far. */
  public List<Map<String, String>> requests() {
    return List.copyOf(requests);
  }
}
