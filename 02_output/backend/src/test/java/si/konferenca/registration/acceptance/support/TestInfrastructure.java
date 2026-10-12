package si.konferenca.registration.acceptance.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared test infrastructure: one PostgreSQL and one Mailpit container per JVM, a JSON copy
 * directory, and the configuration contract of spec section 13 as Spring properties.
 */
public final class TestInfrastructure {

  public static final String CONFERENCE_NAME = "Acceptance Conference 2026";
  public static final String ORGANIZER_USERNAME = "organizer";
  public static final String ORGANIZER_PASSWORD = "acceptance-organizer-pass";
  public static final List<String> ORGANIZER_EMAILS =
      List.of("organizer.one@konferenca.test", "organizer.two@konferenca.test");
  public static final String MAIL_FROM = "registration@konferenca.test";
  public static final String TEST_MODE_TOKEN = "test-mode-token";
  public static final String DEFAULT_CONFIG = "classpath:acceptance/conference-acceptance.json";

  public static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer(DockerImageName.parse("postgres:16.15-alpine"))
          .withDatabaseName("registration")
          .withUsername("registration")
          .withPassword("acceptance-db-password");

  public static final GenericContainer<?> MAILPIT =
      new GenericContainer<>(DockerImageName.parse("axllent/mailpit:v1.31.1"))
          .withExposedPorts(1025, 8025)
          .waitingFor(Wait.forHttp("/livez").forPort(8025));

  public static final Path JSON_COPY_DIR;

  static {
    POSTGRES.start();
    MAILPIT.start();
    try {
      JSON_COPY_DIR = Files.createTempDirectory("acceptance-json-copies");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private TestInfrastructure() {}

  /** Default configuration of a test context, as environment-variable names (spec 13). */
  public static Map<String, Supplier<Object>> defaults() {
    Map<String, Supplier<Object>> p = new LinkedHashMap<>();
    p.put("DATABASE_URL", POSTGRES::getJdbcUrl);
    p.put("DATABASE_USER", POSTGRES::getUsername);
    p.put("POSTGRES_PASSWORD", POSTGRES::getPassword);
    p.put("SMTP_HOST", MAILPIT::getHost);
    p.put("SMTP_PORT", () -> MAILPIT.getMappedPort(1025));
    p.put("SMTP_STARTTLS", () -> "false");
    p.put("REGISTRATION_CONFERENCE_NAME", () -> CONFERENCE_NAME);
    p.put("REGISTRATION_CONFERENCE_CONFIG", () -> DEFAULT_CONFIG);
    p.put("REGISTRATION_JSON_COPY_DIR", JSON_COPY_DIR::toString);
    p.put("REGISTRATION_MAIL_FROM", () -> MAIL_FROM);
    p.put("ORGANIZER_USERNAME", () -> ORGANIZER_USERNAME);
    p.put("ORGANIZER_PASSWORD", () -> ORGANIZER_PASSWORD);
    p.put("ORGANIZER_EMAILS", () -> String.join(",", ORGANIZER_EMAILS));
    p.put("ORGANIZER_HTTPS_ONLY", () -> "true");
    p.put("RECAPTCHA_TEST_MODE", () -> "true");
    p.put("REGISTRATION_RATE_LIMIT_FORM_CONFIG", () -> "100000");
    p.put("REGISTRATION_RATE_LIMIT_REGISTRATIONS", () -> "100000");
    p.put("REGISTRATION_RATE_LIMIT_EXPORT", () -> "100000");
    return p;
  }

  /** Registers the defaults, replaced or extended by {@code overrides}. */
  public static void register(DynamicPropertyRegistry registry, Map<String, Object> overrides) {
    Map<String, Supplier<Object>> p = defaults();
    overrides.forEach((k, v) -> p.put(k, () -> v));
    p.forEach(registry::add);
  }

  /** Same as {@link #register} but as plain values, for starting an application directly. */
  public static Map<String, Object> asMap(Map<String, Object> overrides) {
    Map<String, Object> result = new LinkedHashMap<>();
    defaults().forEach((k, v) -> result.put(k, v.get()));
    result.putAll(overrides);
    return result;
  }

  /** A local TCP port on which nothing listens. */
  public static int closedPort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
