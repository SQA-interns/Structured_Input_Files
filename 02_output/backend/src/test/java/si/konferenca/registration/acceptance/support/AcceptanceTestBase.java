package si.konferenca.registration.acceptance.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import si.konferenca.registration.RegistrationApplication;

/**
 * Starts the application on a random port with PostgreSQL and Mailpit containers, reCAPTCHA test
 * mode and the test conference configuration. Tests use only HTTP; the database, the JSON copy
 * directory and Mailpit are read to observe results.
 */
@SpringBootTest(
    classes = RegistrationApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AcceptanceTestBase {

  protected static final Path CONFERENCE_CONFIG =
      TestEnvironment.copyResource("/acceptance/conference-test.json");
  protected static final Path JSON_COPY_DIR = TestEnvironment.newTempDir("json-copies-");

  @Value("${local.server.port}")
  protected int port;

  protected Api api;
  protected final Db db = new Db();
  protected final Mailpit mailpit = new Mailpit();

  @DynamicPropertySource
  static void baseProperties(DynamicPropertyRegistry registry) {
    TestEnvironment.baseProperties(CONFERENCE_CONFIG, JSON_COPY_DIR)
        .forEach((key, value) -> registry.add(key, () -> value));
  }

  @BeforeEach
  void createClient() {
    api = new Api(port);
  }

  /** Posts the submission and asserts it was accepted; returns the new registration id. */
  protected UUID registerAccepted(Map<String, Object> submission) {
    Api.Response response = api.register(submission);
    assertThat(response.status()).as("status of %s", response.text()).isEqualTo(201);
    return UUID.fromString(response.json().read("$.id"));
  }

  /**
   * Asserts HTTP 400 naming the field, and that nothing was stored for the valid submission the
   * request was derived from.
   */
  protected void assertRejected(
      Api.Response response, String expectedField, Path jsonCopyDir, Map<String, Object> valid) {
    assertThat(response.status()).as("status of %s", response.text()).isEqualTo(400);
    assertThat(response.fieldErrorFields()).contains(expectedField);
    assertNothingStored(jsonCopyDir, valid);
  }

  /** No row and no JSON copy carries the unique email or the unique last name of the submission. */
  protected void assertNothingStored(Path jsonCopyDir, Map<String, Object> valid) {
    String email = (String) valid.get("email");
    String lastName = (String) valid.get("lastName");
    assertThat(db.countByEmail(email)).as("database rows for %s", email).isZero();
    assertThat(db.countByLastName(lastName)).as("database rows for %s", lastName).isZero();
    assertThat(jsonCopiesContaining(jsonCopyDir, email)).as("JSON copies for %s", email).isEmpty();
    assertThat(jsonCopiesContaining(jsonCopyDir, lastName))
        .as("JSON copies for %s", lastName)
        .isEmpty();
  }

  protected static List<Path> jsonCopiesContaining(Path dir, String text) {
    if (!Files.isDirectory(dir)) {
      return List.of();
    }
    try (Stream<Path> files = Files.list(dir)) {
      return files.filter(f -> readString(f).contains(text)).toList();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  protected static Path jsonCopyFile(Path dir, UUID id) {
    return dir.resolve(id + ".json");
  }

  protected static DocumentContext readJsonCopy(Path dir, UUID id) {
    return JsonPath.parse(readString(jsonCopyFile(dir, id)));
  }

  protected static String readString(Path file) {
    try {
      return Files.readString(file, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
