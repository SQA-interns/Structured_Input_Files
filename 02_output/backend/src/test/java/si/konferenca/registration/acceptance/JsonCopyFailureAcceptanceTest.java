package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.Payloads;
import si.konferenca.registration.acceptance.support.TestInfrastructure;

/** US-005: the JSON copy cannot be written (AC-005-03). */
class JsonCopyFailureAcceptanceTest extends AcceptanceTestBase {

  /** A regular file where the directory should be: nothing can be written below it. */
  private static final Path NOT_A_DIRECTORY = createFile();

  private static Path createFile() {
    try {
      return Files.createTempFile("json-copy-dir-is-a-file", ".txt");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @DynamicPropertySource
  static void unwritableCopyDirectory(DynamicPropertyRegistry registry) {
    TestInfrastructure.register(
        registry, Map.of("REGISTRATION_JSON_COPY_DIR", NOT_A_DIRECTORY.toString()));
  }

  @Test
  void AC_005_03_unwritableJsonCopyAnswersErrorAndLeavesNoRegistration() {
    String email = Payloads.uniqueEmail("ac00503");

    HttpResponse<String> response = postRegistration(Payloads.external(email));

    assertThat(response.statusCode()).as(response.body()).isNotEqualTo(201).isEqualTo(500);
    assertThat(json(response.body()).path("message").asString()).isNotBlank();
    assertThat(registrationsWithEmail(email)).isZero();
    assertThat(MAIL.countMentioning(email)).isZero();
  }
}
