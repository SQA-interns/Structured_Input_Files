package si.konferenca.registration.acceptance.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * The test environment of {@code environments.md}: Testcontainers PostgreSQL, Mailpit as mail
 * catcher, a mocked reCAPTCHA endpoint (production code path) and a JSON copy directory. Shared by
 * all acceptance tests of one JVM.
 */
public final class TestEnvironment {

  public static final String POSTGRES_IMAGE = "postgres:16.15-alpine";
  public static final String MAILPIT_IMAGE = "axllent/mailpit:v1.31.1";

  public static final String CONFERENCE_NAME = "Testna konferenca 2026";
  public static final String SENDER = "registration@test.konferenca.si";
  public static final String ORGANIZER_ONE = "org-one@test.konferenca.si";
  public static final String ORGANIZER_TWO = "org-two@test.konferenca.si";
  public static final String ORGANIZER_USERNAME = "test-organizer";
  public static final String ORGANIZER_PASSWORD = "test-organizer-password-0001";
  public static final String SITE_KEY = "test-site-key";

  public static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer(DockerImageName.parse(POSTGRES_IMAGE));

  @SuppressWarnings("resource")
  public static final GenericContainer<?> MAILPIT =
      new GenericContainer<>(DockerImageName.parse(MAILPIT_IMAGE))
          .withExposedPorts(1025, 8025)
          .waitingFor(Wait.forHttp("/api/v1/info").forPort(8025));

  public static final CaptchaMock CAPTCHA = CaptchaMock.start();
  public static final Path JSON_DIR = createTempDirectory("json-copies");

  static {
    POSTGRES.start();
    MAILPIT.start();
  }

  private TestEnvironment() {}

  public static Mailpit mailpit() {
    return new Mailpit("http://" + MAILPIT.getHost() + ":" + MAILPIT.getMappedPort(8025));
  }

  /** Every setting the backend needs in the test environment. */
  public static Map<String, Supplier<Object>> defaults() {
    Map<String, Supplier<Object>> p = new LinkedHashMap<>();
    p.put("app.environment", () -> "test");
    p.put("spring.datasource.url", POSTGRES::getJdbcUrl);
    p.put("spring.datasource.username", POSTGRES::getUsername);
    p.put("spring.datasource.password", POSTGRES::getPassword);
    p.put("spring.mail.host", MAILPIT::getHost);
    p.put("spring.mail.port", () -> MAILPIT.getMappedPort(1025));
    p.put("app.mail.sender", () -> SENDER);
    p.put("app.conference-name", () -> CONFERENCE_NAME);
    p.put("app.organizer.emails", () -> ORGANIZER_ONE + "," + ORGANIZER_TWO);
    p.put("app.organizer.username", () -> ORGANIZER_USERNAME);
    p.put("app.organizer.password", () -> ORGANIZER_PASSWORD);
    p.put("app.organizer.https-only", () -> "true");
    p.put("app.options.location", () -> "classpath:acceptance/conference-options.json");
    p.put("app.json-copy.directory", JSON_DIR::toString);
    p.put("app.recaptcha.test-mode", () -> "false");
    p.put("app.recaptcha.site-key", () -> SITE_KEY);
    p.put("app.recaptcha.secret-key", () -> CaptchaMock.SECRET);
    p.put("app.recaptcha.verify-url", CAPTCHA::url);
    p.put("app.request.max-body-bytes", () -> "16384");
    return p;
  }

  /** Registers the defaults, replaced or extended by {@code overrides}. */
  public static void register(
      DynamicPropertyRegistry registry, Map<String, Supplier<Object>> overrides) {
    Map<String, Supplier<Object>> all = defaults();
    all.putAll(overrides);
    all.forEach(registry::add);
  }

  /** The same settings as command-line arguments, for starting the application directly. */
  public static String[] asArguments(Map<String, Supplier<Object>> overrides) {
    Map<String, Supplier<Object>> all = defaults();
    all.putAll(overrides);
    return all.entrySet().stream()
        .map(e -> "--" + e.getKey() + "=" + e.getValue().get())
        .toArray(String[]::new);
  }

  public static Path createTempDirectory(String prefix) {
    try {
      return Files.createTempDirectory(prefix);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
