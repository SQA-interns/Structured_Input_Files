package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static si.konferenca.registration.acceptance.support.Payloads.external;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import si.konferenca.registration.acceptance.support.AcceptanceTestSupport;
import si.konferenca.registration.acceptance.support.Api;
import si.konferenca.registration.acceptance.support.TestEnvironment;

/** US-005: a registration is accepted only when its JSON copy was written (AR-05). */
class JsonCopyFailureAcceptanceTest extends AcceptanceTestSupport {

  private static final Path OWN_JSON_DIR = TestEnvironment.newTempDir("json-copies-failing-");

  @DynamicPropertySource
  static void ownJsonCopyDir(DynamicPropertyRegistry registry) {
    Map<String, String> p = TestEnvironment.baseProperties(CONFERENCE_CONFIG, OWN_JSON_DIR);
    p.forEach((key, value) -> registry.add(key, () -> value));
  }

  @Test
  @DisplayName("AC-005-03: when the JSON copy cannot be written, nothing is accepted or kept")
  void ac_005_03_failedJsonCopyLeavesNoRegistration() throws IOException {
    Map<String, Object> submission = external();
    // The started application owns the directory; replace it by a plain file so writing fails.
    deleteRecursively(OWN_JSON_DIR);
    Files.writeString(OWN_JSON_DIR, "not a directory");
    try {
      Api.Response response = api.register(submission);

      assertThat(response.status()).as(response.text()).isNotEqualTo(201).isBetween(500, 599);
      assertThat(db.countByEmail((String) submission.get("email"))).isZero();
      assertThat(db.countByLastName((String) submission.get("lastName"))).isZero();
    } finally {
      Files.deleteIfExists(OWN_JSON_DIR);
      Files.createDirectories(OWN_JSON_DIR);
    }
  }

  private static void deleteRecursively(Path dir) throws IOException {
    if (!Files.exists(dir)) {
      return;
    }
    try (Stream<Path> paths = Files.walk(dir)) {
      for (Path p : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
        Files.delete(p);
      }
    }
  }
}
