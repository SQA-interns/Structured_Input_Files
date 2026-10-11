package si.konferenca.registration.acceptance.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared containers and properties of the acceptance tests. Images and versions come from
 * tech-stack.md; property names from specification section 3.
 */
public final class TestEnvironment {

  public static final String CONFERENCE_NAME = "Konferenca 2026";
  public static final String ORGANIZER_USERNAME = "organizer";
  public static final String ORGANIZER_PASSWORD = "acceptance-test-organizer-password";
  public static final String ORGANIZER_EMAIL_1 = "organizer.one@example.test";
  public static final String ORGANIZER_EMAIL_2 = "organizer.two@example.test";
  public static final String TEST_CAPTCHA_TOKEN = "TEST-PASS";

  public static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer(DockerImageName.parse("postgres:16.15-alpine"));

  @SuppressWarnings("resource")
  public static final GenericContainer<?> MAILPIT =
      new GenericContainer<>(DockerImageName.parse("axllent/mailpit:v1.31.1"))
          .withExposedPorts(1025, 8025)
          .waitingFor(Wait.forHttp("/api/v1/messages").forPort(8025));

  static {
    POSTGRES.start();
    MAILPIT.start();
  }

  private TestEnvironment() {}

  /** Base properties of a test-mode application using the given configuration and directory. */
  public static Map<String, String> baseProperties(Path conferenceConfig, Path jsonCopyDir) {
    Map<String, String> p = new LinkedHashMap<>();
    p.put("spring.datasource.url", POSTGRES.getJdbcUrl());
    p.put("spring.datasource.username", POSTGRES.getUsername());
    p.put("spring.datasource.password", POSTGRES.getPassword());
    p.put("spring.mail.host", MAILPIT.getHost());
    p.put("spring.mail.port", String.valueOf(MAILPIT.getMappedPort(1025)));
    p.put("app.mail.starttls", "false");
    p.put("app.environment", "test");
    p.put("app.recaptcha.test-mode", "true");
    p.put("app.conference.name", CONFERENCE_NAME);
    p.put("app.conference.config-file", conferenceConfig.toString());
    p.put("app.json-copy-dir", jsonCopyDir.toString());
    p.put("app.organizer.username", ORGANIZER_USERNAME);
    p.put("app.organizer.password", ORGANIZER_PASSWORD);
    p.put("app.organizer.emails", ORGANIZER_EMAIL_1 + "," + ORGANIZER_EMAIL_2);
    p.put("app.rate-limit.registration-per-minute", "100000");
    p.put("app.rate-limit.form-config-per-minute", "100000");
    p.put("app.rate-limit.export-per-minute", "100000");
    return p;
  }

  /** Copies a classpath resource to a new temporary file and returns its path. */
  public static Path copyResource(String resource) {
    try (InputStream in = TestEnvironment.class.getResourceAsStream(resource)) {
      if (in == null) {
        throw new IllegalArgumentException("missing test resource " + resource);
      }
      Path file = Files.createTempFile("conference-", ".json");
      Files.write(file, in.readAllBytes());
      return file;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static Path newTempDir(String prefix) {
    try {
      return Files.createTempDirectory(prefix);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** A local TCP port with nothing listening on it. */
  public static int unusedPort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
