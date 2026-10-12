package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.TestStack;

/** AC-005-03: when the JSON copy cannot be written, no registration is left behind. */
class JsonCopyFailureAcceptanceTest extends AcceptanceTestBase {

  /** A regular file; the configured copy directory lies below it and can never be created. */
  private static final Path BLOCKING_FILE = blockingFile();

  private static final Path UNWRITABLE_DIR = BLOCKING_FILE.resolve("copies");

  @DynamicPropertySource
  static void settings(DynamicPropertyRegistry registry) {
    TestStack.register(registry, Map.of("JSON_COPY_DIR", UNWRITABLE_DIR.toString()));
  }

  @Override
  protected Path jsonCopyDir() {
    return UNWRITABLE_DIR;
  }

  @Test
  @DisplayName(
      "AC-005-03 JSON copy not writable: error response and no registration in the database")
  void ac_005_03_unwritableJsonCopyLeavesNoRegistration() throws Exception {
    MvcResult result = submit(validExternal());

    assertThat(result.getResponse().getStatus()).as(body(result)).isNotEqualTo(201);
    assertThat(result.getResponse().getStatus()).as(body(result)).isGreaterThanOrEqualTo(400);
    assertErrorBody(result);
    assertThat(registrationCount()).as("registrations in the database").isZero();
    assertThat(Files.exists(UNWRITABLE_DIR)).isFalse();
    Thread.sleep(300);
    assertThat(mailpit.total()).as("emails sent").isZero();
  }

  @Test
  @DisplayName("AC-005-03 JSON copy not writable: student registration is not left in the database")
  void ac_005_03_unwritableJsonCopyLeavesNoStudentRegistration() throws Exception {
    MvcResult result = submit(validStudent());

    assertThat(result.getResponse().getStatus()).as(body(result)).isNotEqualTo(201);
    assertThat(result.getResponse().getStatus()).as(body(result)).isGreaterThanOrEqualTo(400);
    assertErrorBody(result);
    assertThat(registrationCount()).as("registrations in the database").isZero();
  }

  /** Every error answer carries the Error schema of the API contract (openapi.json). */
  private static void assertErrorBody(MvcResult result) throws Exception {
    assertThat(body(result)).as("error body").isNotBlank();
    String message = JsonPath.read(body(result), "$.message");
    assertThat(message).isNotBlank();
  }

  private static Path blockingFile() {
    try {
      Path file = Files.createTempFile("json-copy-blocker", ".txt");
      Files.writeString(file, "not a directory");
      return file;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
