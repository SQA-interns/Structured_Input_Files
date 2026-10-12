package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.TestStack;

/** AC-006-03: an unavailable mail server does not affect the accepted registration. */
class MailServerUnavailableAcceptanceTest extends AcceptanceTestBase {

  @DynamicPropertySource
  static void settings(DynamicPropertyRegistry registry) {
    TestStack.register(
        registry, Map.of("SMTP_HOST", "127.0.0.1", "SMTP_PORT", String.valueOf(closedPort())));
  }

  @Test
  @DisplayName("AC-006-03 mail server unavailable: registration still accepted (201) and stored")
  void ac_006_03_registrationIsAcceptedWithoutMailServer() throws Exception {
    String id = register(validExternal());

    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM registration WHERE id = ?::uuid", Integer.class, id))
        .isEqualTo(1);
    assertThat(Files.isRegularFile(jsonCopy(id))).isTrue();
  }

  @Test
  @DisplayName("AC-006-03 mail server unavailable: student registration still accepted and stored")
  void ac_006_03_studentRegistrationIsAcceptedWithoutMailServer() throws Exception {
    String id = register(validStudent());

    assertThat(registrationCount()).isEqualTo(1);
    assertThat(Files.isRegularFile(jsonCopy(id))).isTrue();
  }

  /** A local port that nothing listens on. */
  private static int closedPort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
