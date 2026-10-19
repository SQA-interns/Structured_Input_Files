package si.konferenca.registration.acceptance.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * The test environment of the acceptance tests: PostgreSQL and Mailpit containers (images from
 * tech-stack.md), started once per JVM, and the backend settings of configuration.md.
 */
public final class AcceptanceEnvironment {

  public static final String ORGANIZER_USERNAME = "organizer";
  public static final String ORGANIZER_PASSWORD = "Organizer-Test-Password-2026";
  public static final String ORGANIZER_ONE = "organizer.one@konferenca.test";
  public static final String ORGANIZER_TWO = "organizer.two@konferenca.test";
  public static final String CONFERENCE_NAME = "Konferenca Test 2026";
  public static final String MAIL_FROM = "registration@konferenca.test";
  public static final String VALID_CAPTCHA = "test-pass";

  private static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer(DockerImageName.parse("postgres:16.15-alpine"));

  @SuppressWarnings("resource")
  private static final GenericContainer<?> MAILPIT =
      new GenericContainer<>(DockerImageName.parse("axllent/mailpit:v1.31.1"))
          .withExposedPorts(1025, 8025)
          .waitingFor(Wait.forHttp("/api/v1/info").forPort(8025));

  private static final Path DEFAULT_JSON_COPY_DIR = newTempDir("json-copies");

  static {
    POSTGRES.start();
    MAILPIT.start();
  }

  private AcceptanceEnvironment() {}

  /** Path of a file under src/test/resources/acceptance. */
  public static Path resource(String name) {
    return Path.of("src", "test", "resources", "acceptance", name).toAbsolutePath();
  }

  public static Path defaultJsonCopyDir() {
    return DEFAULT_JSON_COPY_DIR;
  }

  public static Path newTempDir(String prefix) {
    try {
      return Files.createTempDirectory(prefix);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** The settings of configuration.md for the test environment, before any override. */
  public static Map<String, String> defaultProperties() {
    Map<String, String> p = new LinkedHashMap<>();
    p.put("app.environment", "test");
    p.put("spring.datasource.url", POSTGRES.getJdbcUrl());
    p.put("spring.datasource.username", POSTGRES.getUsername());
    p.put("spring.datasource.password", POSTGRES.getPassword());
    p.put("spring.mail.host", MAILPIT.getHost());
    p.put("spring.mail.port", String.valueOf(MAILPIT.getMappedPort(1025)));
    p.put("app.mail.tls", "false");
    p.put("app.mail.from", MAIL_FROM);
    p.put("app.organizer.emails", ORGANIZER_ONE + "," + ORGANIZER_TWO);
    p.put("app.organizer.username", ORGANIZER_USERNAME);
    p.put("app.organizer.password", ORGANIZER_PASSWORD);
    p.put("app.organizer.https-only", "true");
    p.put("app.conference.config-file", resource("conference-test.json").toString());
    p.put("app.json-copy.dir", DEFAULT_JSON_COPY_DIR.toString());
    p.put("app.recaptcha.test-mode", "true");
    p.put("app.rate-limit.form-config-per-minute", "100000");
    p.put("app.rate-limit.registration-per-minute", "100000");
    p.put("app.rate-limit.export-per-minute", "100000");
    return p;
  }

  public static void register(DynamicPropertyRegistry registry, Map<String, String> overrides) {
    Map<String, String> p = defaultProperties();
    p.putAll(overrides);
    p.forEach((key, value) -> registry.add(key, () -> value));
  }

  /** Command-line arguments with the same settings, for starting the application directly. */
  public static String[] asArguments(Map<String, String> overrides) {
    Map<String, String> p = defaultProperties();
    p.putAll(overrides);
    p.put("server.port", "0");
    return p.entrySet().stream()
        .map(e -> "--" + e.getKey() + "=" + e.getValue())
        .toArray(String[]::new);
  }

  /** Direct SQL access to the registration storage (database.sql contract). */
  public static JdbcTemplate jdbc() {
    return new JdbcTemplate(
        new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
  }

  public static MailpitClient mailpit() {
    return new MailpitClient("http://" + MAILPIT.getHost() + ":" + MAILPIT.getMappedPort(8025));
  }
}
