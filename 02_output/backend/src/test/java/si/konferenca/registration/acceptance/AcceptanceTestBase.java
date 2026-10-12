package si.konferenca.registration.acceptance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import si.konferenca.registration.RegistrationApplication;

/**
 * Shared harness for the backend acceptance tests: the real application on a random port against
 * PostgreSQL and Mailpit containers, configured only through the variables of the specification
 * (S11). Tests observe the API, the database (contract {@code database.md}), the JSON copy
 * directory and the delivered emails.
 */
@SpringBootTest(
    classes = RegistrationApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AcceptanceTestBase {

  static final String POSTGRES_IMAGE = "postgres:16.15-alpine";
  static final String MAILPIT_IMAGE = "axllent/mailpit:v1.31.1";

  static final String ORGANIZER_USERNAME = "organizer";
  static final String ORGANIZER_PASSWORD = UUID.randomUUID().toString();
  static final String ORGANIZER_EMAIL_1 = "organizer.one@test.local";
  static final String ORGANIZER_EMAIL_2 = "organizer.two@test.local";
  static final String CONFERENCE_NAME = "Test Conference 2026";

  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE);
  static final GenericContainer<?> MAILPIT = mailpit();
  static final Path COPY_DIR;

  static {
    POSTGRES.start();
    MAILPIT.start();
    try {
      COPY_DIR = Files.createTempDirectory("registration-copies");
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @LocalServerPort int port;

  protected Api api;
  protected Db db;
  protected Mailbox mailbox;
  protected CopyDir copies;

  static GenericContainer<?> mailpit() {
    return new GenericContainer<>(MAILPIT_IMAGE)
        .withExposedPorts(1025, 8025)
        .waitingFor(Wait.forHttp("/livez").forPort(8025));
  }

  @DynamicPropertySource
  static void configure(DynamicPropertyRegistry registry) {
    registerDatabase(registry, POSTGRES);
    registerMail(registry, MAILPIT.getHost(), MAILPIT.getMappedPort(1025));
    registerCommon(registry);
    registry.add("JSON_COPY_DIR", COPY_DIR::toString);
  }

  static void registerDatabase(DynamicPropertyRegistry registry, PostgreSQLContainer postgres) {
    registry.add("DB_URL", postgres::getJdbcUrl);
    registry.add("DB_USERNAME", postgres::getUsername);
    registry.add("POSTGRES_PASSWORD", postgres::getPassword);
    // Harness only: lets the bootstrap skeleton start before the configuration exists.
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  static void registerMail(DynamicPropertyRegistry registry, String host, int smtpPort) {
    registry.add("MAIL_HOST", () -> host);
    registry.add("MAIL_PORT", () -> smtpPort);
    registry.add("MAIL_TLS", () -> "false");
    registry.add("MAIL_FROM", () -> "registration@test.local");
  }

  static void registerCommon(DynamicPropertyRegistry registry) {
    registry.add("CONFERENCE_NAME", () -> CONFERENCE_NAME);
    registry.add("OPTIONS_LOCATION", () -> "classpath:acceptance/options.json");
    registry.add("RECAPTCHA_TEST_MODE", () -> "true");
    registry.add("ORGANIZER_USERNAME", () -> ORGANIZER_USERNAME);
    registry.add("ORGANIZER_PASSWORD", () -> ORGANIZER_PASSWORD);
    registry.add("ORGANIZER_EMAILS", () -> ORGANIZER_EMAIL_1 + "," + ORGANIZER_EMAIL_2);
    registry.add("RATE_LIMIT_REGISTRATIONS_PER_MINUTE", () -> "100000");
    registry.add("RATE_LIMIT_READS_PER_MINUTE", () -> "100000");
    registry.add("RATE_LIMIT_ORGANIZER_PER_MINUTE", () -> "100000");
  }

  @BeforeEach
  void resetState() throws Exception {
    api = new Api("http://localhost:" + port);
    db = new Db(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    mailbox = new Mailbox("http://" + MAILPIT.getHost() + ":" + MAILPIT.getMappedPort(8025));
    copies = new CopyDir(COPY_DIR);
    db.deleteAllRegistrations();
    mailbox.deleteAll();
    copies.reset();
  }

  /** Asserts the outcome every rejection shares: nothing stored, copied or sent. */
  protected void assertNothingStoredCopiedOrSent() throws Exception {
    org.assertj.core.api.Assertions.assertThat(db.countRegistrations())
        .as("registrations in the database")
        .isZero();
    org.assertj.core.api.Assertions.assertThat(copies.files()).as("JSON copies").isEmpty();
    org.assertj.core.api.Assertions.assertThat(mailbox.messagesAfterSettling())
        .as("emails sent")
        .isEmpty();
  }
}
