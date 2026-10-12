package si.konferenca.registration.acceptance.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Local substitutes of the test environment (environments.md): PostgreSQL and the Mailpit mail
 * catcher, started once per JVM and shared by every acceptance test context.
 */
public final class TestStack {

  public static final String CONFERENCE_NAME = "Test Conference 2026";
  public static final String ORGANIZER_USERNAME = "organizer";
  public static final String ORGANIZER_PASSWORD = "acceptance-organizer-pass-0001";
  public static final String ORGANIZER_EMAIL_1 = "organizer.one@example.org";
  public static final String ORGANIZER_EMAIL_2 = "organizer.two@example.org";
  public static final String TEST_MODE_TOKEN = "test-pass";

  public static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16.15-alpine")
          .withDatabaseName("registration")
          .withUsername("registration")
          .withPassword("acceptance-db-pass")
          // Every cached test context keeps its own connection pool.
          .withCommand("postgres", "-c", "max_connections=300", "-c", "fsync=off");

  @SuppressWarnings("resource")
  public static final GenericContainer<?> MAILPIT =
      new GenericContainer<>(DockerImageName.parse("axllent/mailpit:v1.31.1"))
          .withExposedPorts(1025, 8025)
          .waitingFor(Wait.forHttp("/api/v1/info").forPort(8025));

  /** JSON copy directory shared by the contexts that use the default storage location. */
  public static final Path JSON_COPY_DIR = tempDir("json-copies");

  static {
    POSTGRES.start();
    MAILPIT.start();
  }

  private TestStack() {}

  public static Mailpit mailpit() {
    return new Mailpit("http://" + MAILPIT.getHost() + ":" + MAILPIT.getMappedPort(8025));
  }

  /** Registers the settings of specification section 12 for the test environment. */
  public static void register(DynamicPropertyRegistry registry) {
    register(registry, Map.of());
  }

  /**
   * Registers the test settings; {@code overrides} replaces or adds settings (environment-variable
   * names of specification section 12).
   */
  public static void register(DynamicPropertyRegistry registry, Map<String, Object> overrides) {
    Map<String, Object> settings = new java.util.LinkedHashMap<>();
    settings.put("DB_URL", POSTGRES.getJdbcUrl());
    settings.put("DB_USER", POSTGRES.getUsername());
    settings.put("POSTGRES_PASSWORD", POSTGRES.getPassword());
    settings.put("spring.datasource.url", POSTGRES.getJdbcUrl());
    settings.put("spring.datasource.username", POSTGRES.getUsername());
    settings.put("spring.datasource.password", POSTGRES.getPassword());
    settings.put("spring.datasource.hikari.maximum-pool-size", "4");
    settings.put("spring.datasource.hikari.minimum-idle", "1");
    settings.put("SMTP_HOST", MAILPIT.getHost());
    settings.put("SMTP_PORT", MAILPIT.getMappedPort(1025));
    settings.put("SMTP_STARTTLS", "false");
    settings.put("MAIL_FROM", "registration@konferenca.example");
    settings.put("CONFERENCE_NAME", CONFERENCE_NAME);
    settings.put("CONFERENCE_CONFIG", "classpath:acceptance/conference-acceptance.json");
    settings.put("JSON_COPY_DIR", JSON_COPY_DIR.toString());
    settings.put("APP_ENVIRONMENT", "test");
    settings.put("RECAPTCHA_TEST_MODE", "true");
    settings.put("RECAPTCHA_SITE_KEY", "");
    settings.put("RECAPTCHA_SECRET_KEY", "");
    settings.put("ORGANIZER_USERNAME", ORGANIZER_USERNAME);
    settings.put("ORGANIZER_PASSWORD", ORGANIZER_PASSWORD);
    settings.put("ORGANIZER_EMAILS", ORGANIZER_EMAIL_1 + "," + ORGANIZER_EMAIL_2);
    settings.put("RATE_LIMIT_FORM_CONFIG", "100000");
    settings.put("RATE_LIMIT_REGISTRATIONS", "100000");
    settings.put("RATE_LIMIT_EXPORT", "100000");
    settings.putAll(overrides);
    settings.forEach((name, value) -> registry.add(name, () -> value));
  }

  /** The same settings as plain command-line properties, for starting the application directly. */
  public static String[] commandLine(Map<String, Object> overrides) {
    Map<String, Object> settings = new java.util.LinkedHashMap<>();
    register(
        new DynamicPropertyRegistry() {
          @Override
          public void add(String name, java.util.function.Supplier<Object> supplier) {
            settings.put(name, supplier.get());
          }
        },
        overrides);
    settings.put("server.port", "0");
    return settings.entrySet().stream()
        .map(e -> "--" + e.getKey() + "=" + e.getValue())
        .toArray(String[]::new);
  }

  public static Path tempDir(String prefix) {
    try {
      return Files.createTempDirectory(prefix);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
