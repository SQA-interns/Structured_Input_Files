package si.konferenca.registration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import si.konferenca.registration.acceptance.support.AcceptanceEnvironment;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationSubmission;

/** AR-05: the database row and the JSON copy are written in one transaction. */
@SpringBootTest
class RegistrationTransactionIntegrationTest {

  private static final Path COPY_DIR = AcceptanceEnvironment.newTempDir("json-copies-tx");

  @Autowired RegisterParticipant registerParticipant;

  private final JdbcTemplate jdbc = AcceptanceEnvironment.jdbc();

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    AcceptanceEnvironment.register(registry, Map.of("app.json-copy.dir", COPY_DIR.toString()));
  }

  @BeforeEach
  @AfterEach
  void clean() {
    AcceptanceTestBase.clearStorage(jdbc);
    AcceptanceTestBase.clearDirectory(COPY_DIR);
  }

  static RegistrationSubmission submission() {
    return new RegistrationSubmission(
        "EXTERNAL",
        "Ana",
        "Novak",
        "ana@example.com",
        "IJS",
        null,
        null,
        null,
        List.of("ws-ai"),
        List.of("privacy"),
        "test-pass");
  }

  private int rows() {
    return jdbc.queryForObject("SELECT COUNT(*) FROM registration", Integer.class);
  }

  @Test
  void AC_005_02_acceptedRegistrationHasRowAndCopy() {
    Registration registration = registerParticipant.register(submission());

    assertThat(rows()).isEqualTo(1);
    assertThat(COPY_DIR.resolve(registration.id() + ".json")).isRegularFile();
  }

  @Test
  void AC_005_03_copyFailureRollsTheDatabaseRowBack() throws Exception {
    Files.delete(COPY_DIR);
    Files.writeString(COPY_DIR, "not a directory");

    assertThatThrownBy(() -> registerParticipant.register(submission()))
        .isInstanceOf(RuntimeException.class);
    assertThat(rows()).isZero();
  }
}
