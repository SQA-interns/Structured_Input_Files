package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import si.konferenca.registration.RegistrationApplication;

/** US-005 AC-005-04, with a database of its own that the test stops. */
@SpringBootTest(
    classes = RegistrationApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class US005DatabaseUnavailableAcceptanceTest {

  static final PostgreSQLContainer OWN_POSTGRES =
      new PostgreSQLContainer(AcceptanceTestBase.POSTGRES_IMAGE);
  static final Path OWN_COPY_DIR;

  static {
    OWN_POSTGRES.start();
    try {
      OWN_COPY_DIR = Files.createTempDirectory("registration-copies-db-down");
    } catch (java.io.IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @LocalServerPort int port;

  @DynamicPropertySource
  static void configure(DynamicPropertyRegistry registry) {
    AcceptanceTestBase.registerDatabase(registry, OWN_POSTGRES);
    AcceptanceTestBase.registerMail(
        registry,
        AcceptanceTestBase.MAILPIT.getHost(),
        AcceptanceTestBase.MAILPIT.getMappedPort(1025));
    AcceptanceTestBase.registerCommon(registry);
    registry.add("JSON_COPY_DIR", OWN_COPY_DIR::toString);
    // Harness only: fail fast instead of waiting 30 s for a connection.
    registry.add("spring.datasource.hikari.connection-timeout", () -> "3000");
  }

  @Test
  @DisplayName("AC-005-04 when the database is unavailable nothing is copied or sent")
  void ac005_04_databaseFailureRejectsTheRegistration() throws Exception {
    Mailbox mailbox =
        new Mailbox(
            "http://"
                + AcceptanceTestBase.MAILPIT.getHost()
                + ":"
                + AcceptanceTestBase.MAILPIT.getMappedPort(8025));
    mailbox.deleteAll();
    OWN_POSTGRES.stop();

    Response response =
        new Api("http://localhost:" + port).postRegistration(Registrations.validExternal());

    assertThat(response.status()).as(response.toString()).isEqualTo(500);
    assertThat(new CopyDir(OWN_COPY_DIR).files()).isEmpty();
    assertThat(mailbox.messagesAfterSettling()).isEmpty();
  }
}
