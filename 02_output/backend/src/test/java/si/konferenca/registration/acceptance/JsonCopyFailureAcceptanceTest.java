package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** US-005: the JSON copy directory is a regular file, so no copy can be written. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JsonCopyFailureAcceptanceTest {

  private static final Path NOT_A_DIRECTORY;

  static {
    try {
      NOT_A_DIRECTORY = Files.createTempFile("json-copy-dir-is-a-file", ".txt");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @LocalServerPort private int port;

  private Api api;

  @DynamicPropertySource
  static void backendProperties(DynamicPropertyRegistry registry) {
    AcceptanceEnvironment.register(
        registry,
        AcceptanceEnvironment.Settings.defaults().withJsonCopyDir(NOT_A_DIRECTORY.toString()));
  }

  @BeforeEach
  void resetState() {
    AcceptanceEnvironment.reset();
    api = new Api(port);
  }

  @Test
  @DisplayName(
      "AC-005-03 when the JSON copy cannot be written the API answers an error and stores nothing")
  void ac00503JsonCopyFailureLeavesNoRegistration() {
    Api.Response response = api.postRegistration(Submissions.validExternal());

    // A storage failure is a server error (spec §4 step 5), never 201 and never a client error.
    assertThat(response.status()).as("status").isNotEqualTo(201).isBetween(500, 599);
    assertThat(AcceptanceEnvironment.registrationCount()).as("database rows").isZero();
  }
}
