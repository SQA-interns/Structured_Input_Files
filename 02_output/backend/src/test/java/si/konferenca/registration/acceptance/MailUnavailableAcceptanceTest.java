package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import si.konferenca.registration.acceptance.support.AcIdDisplayNames;
import si.konferenca.registration.acceptance.support.AcceptanceSupport;
import si.konferenca.registration.acceptance.support.TestEnvironment;
import tools.jackson.databind.JsonNode;

/** AC-006-03: the mail server is unavailable (nothing listens on the SMTP port). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayNameGeneration(AcIdDisplayNames.class)
class MailUnavailableAcceptanceTest extends AcceptanceSupport {

  private static final int CLOSED_PORT = freePort();

  @LocalServerPort private int port;

  @DynamicPropertySource
  static void environment(DynamicPropertyRegistry registry) {
    Map<String, Supplier<Object>> overrides =
        Map.of(
            "spring.mail.host", () -> "127.0.0.1",
            "spring.mail.port", () -> CLOSED_PORT);
    TestEnvironment.register(registry, overrides);
  }

  @Override
  protected int port() {
    return port;
  }

  @Test
  void ac_006_03_registrationIsAcceptedAndStoredWhenMailFails() {
    String email = uniqueEmail("ac-006-03");

    HttpResponse<String> response = register(validExternal(email));

    assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
    JsonNode accepted = json(response);
    String reference = accepted.path("reference").asString();
    assertThat(registrationRow(reference)).containsEntry("email", email);
    assertThat(jsonCopy(TestEnvironment.JSON_DIR, reference)).exists();
  }

  private static int freePort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
