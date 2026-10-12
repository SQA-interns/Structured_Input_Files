package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import si.konferenca.registration.RegistrationApplication;

/** US-006 AC-006-04: the SMTP port points to a port where nothing listens. */
@SpringBootTest(
    classes = RegistrationApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class US006MailServerUnavailableAcceptanceTest {

  static final Path OWN_COPY_DIR;
  static final int CLOSED_PORT;

  static {
    try {
      OWN_COPY_DIR = Files.createTempDirectory("registration-copies-mail-down");
      try (ServerSocket socket = new ServerSocket(0)) {
        CLOSED_PORT = socket.getLocalPort();
      }
    } catch (java.io.IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @LocalServerPort int port;

  @DynamicPropertySource
  static void configure(DynamicPropertyRegistry registry) {
    AcceptanceTestBase.registerDatabase(registry, AcceptanceTestBase.POSTGRES);
    AcceptanceTestBase.registerMail(registry, "localhost", CLOSED_PORT);
    AcceptanceTestBase.registerCommon(registry);
    registry.add("JSON_COPY_DIR", OWN_COPY_DIR::toString);
  }

  @Test
  @DisplayName("AC-006-04 with the mail server unavailable the registration is accepted and kept")
  void ac006_04_mailFailureKeepsTheRegistration() throws Exception {
    Db db =
        new Db(
            AcceptanceTestBase.POSTGRES.getJdbcUrl(),
            AcceptanceTestBase.POSTGRES.getUsername(),
            AcceptanceTestBase.POSTGRES.getPassword());
    db.deleteAllRegistrations();

    Response response =
        new Api("http://localhost:" + port).postRegistration(Registrations.validExternal());

    assertThat(response.status()).as(response.toString()).isEqualTo(201);
    String id = response.json("$.id");
    assertThat(db.registration(id)).isNotNull();
    assertThat(new CopyDir(OWN_COPY_DIR).copyOf(id)).exists();
  }
}
