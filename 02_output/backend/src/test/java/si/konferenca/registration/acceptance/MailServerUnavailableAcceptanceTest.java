package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** US-006: the configured SMTP server does not accept connections (a closed local port). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MailServerUnavailableAcceptanceTest {

  private static final int CLOSED_PORT;

  static {
    try (ServerSocket socket = new ServerSocket(0)) {
      CLOSED_PORT = socket.getLocalPort();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @LocalServerPort private int port;

  private Api api;

  @DynamicPropertySource
  static void backendProperties(DynamicPropertyRegistry registry) {
    AcceptanceEnvironment.register(
        registry,
        AcceptanceEnvironment.Settings.defaults().withMailServer("127.0.0.1", CLOSED_PORT));
  }

  @BeforeEach
  void resetState() {
    AcceptanceEnvironment.reset();
    api = new Api(port);
  }

  @Test
  @DisplayName("AC-006-03 with the mail server unavailable the registration is accepted and stored")
  void ac00603AcceptedWithoutMailServer() {
    Api.Response response = api.postRegistration(Submissions.validExternal());

    assertThat(response.status()).as("status, body: %s", response.text()).isEqualTo(201);
    String id = response.json().get("id").asString();
    assertThat(AcceptanceEnvironment.query("SELECT id FROM registration WHERE id = ?::uuid", id))
        .hasSize(1);
    assertThat(AcceptanceEnvironment.jsonCopy(id)).exists();
  }
}
