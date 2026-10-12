package si.konferenca.registration.acceptance.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import si.konferenca.registration.RegistrationApplication;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Starts the backend on a random port and talks to it only through its public interfaces: HTTP, the
 * database schema contract, the JSON copy directory and the SMTP server (Mailpit).
 *
 * <p>Each concrete test class (or an abstract parent) declares a {@code @DynamicPropertySource}
 * method that calls {@link TestInfrastructure#register}.
 */
@SpringBootTest(
    classes = RegistrationApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AcceptanceTestBase {

  protected static final JsonMapper JSON = JsonMapper.builder().build();
  protected static final MailpitClient MAIL =
      new MailpitClient(
          "http://"
              + TestInfrastructure.MAILPIT.getHost()
              + ":"
              + TestInfrastructure.MAILPIT.getMappedPort(8025));
  protected static final JdbcTemplate DB =
      new JdbcTemplate(
          new DriverManagerDataSource(
              TestInfrastructure.POSTGRES.getJdbcUrl(),
              TestInfrastructure.POSTGRES.getUsername(),
              TestInfrastructure.POSTGRES.getPassword()));

  private final HttpClient http = HttpClient.newHttpClient();

  @LocalServerPort protected int port;

  protected URI uri(String path) {
    return URI.create("http://localhost:" + port + path);
  }

  protected HttpResponse<String> postRegistration(Map<String, Object> body) {
    return postJson("/api/registrations", JSON.writeValueAsString(body));
  }

  protected HttpResponse<String> postJson(String path, String json) {
    return send(
        HttpRequest.newBuilder(uri(path))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  protected HttpResponse<String> get(String path) {
    return send(
        HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
  }

  protected HttpResponse<byte[]> getBytes(String path, Map<String, String> headers) {
    HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path)).GET();
    headers.forEach(builder::header);
    return send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
  }

  protected static String basicAuth(String user, String password) {
    return "Basic "
        + Base64.getEncoder()
            .encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
  }

  private <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) {
    try {
      return http.send(request, handler);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  protected static JsonNode json(String body) {
    return JSON.readTree(body);
  }

  /** Registers successfully and returns the new registration id. */
  protected String registerOk(Map<String, Object> body) {
    HttpResponse<String> response = postRegistration(body);
    assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
    return json(response.body()).path("id").asString();
  }

  /** Asserts a 400 answer whose field errors contain {@code fieldKey}. */
  protected static void assertRejectedWithFieldError(HttpResponse<String> response, String key) {
    assertThat(response.statusCode()).as(response.body()).isEqualTo(400);
    JsonNode fieldErrors = json(response.body()).path("fieldErrors");
    assertThat(fieldErrors.has(key)).as("field error for %s in %s", key, response.body()).isTrue();
    assertThat(fieldErrors.path(key).asString()).isNotBlank();
  }

  protected static int registrationsWithEmail(String email) {
    Integer count =
        DB.queryForObject(
            "SELECT count(*) FROM registration WHERE email = ?", Integer.class, email);
    return count == null ? 0 : count;
  }

  protected static int registrationCount() {
    Integer count = DB.queryForObject("SELECT count(*) FROM registration", Integer.class);
    return count == null ? 0 : count;
  }

  protected static Path copyFile(String id) {
    return TestInfrastructure.JSON_COPY_DIR.resolve(id + ".json");
  }

  protected static List<Path> copiesMentioning(String text) {
    try (Stream<Path> files = Files.list(TestInfrastructure.JSON_COPY_DIR)) {
      return files.filter(f -> readUtf8(f).contains(text)).toList();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  protected static String readUtf8(Path file) {
    try {
      return Files.readString(file, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** "Rejected": no database row, no JSON copy and no email for the submission's address. */
  protected static void assertNothingStoredOrSent(String email) {
    assertThat(registrationsWithEmail(email)).as("database rows for %s", email).isZero();
    assertThat(copiesMentioning(email)).as("JSON copies for %s", email).isEmpty();
    assertThat(MAIL.countMentioning(email)).as("emails mentioning %s", email).isZero();
  }
}
