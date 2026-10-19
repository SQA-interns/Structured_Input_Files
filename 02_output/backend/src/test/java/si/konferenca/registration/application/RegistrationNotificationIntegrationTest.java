package si.konferenca.registration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import si.konferenca.registration.acceptance.support.AcceptanceEnvironment;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.domain.Registration;

/** OQ-03: emails start only after the row and the JSON copy were committed. */
@SpringBootTest
class RegistrationNotificationIntegrationTest {

  private static final Path COPY_DIR = AcceptanceEnvironment.newTempDir("json-copies-notify");
  private static final List<Integer> ROWS_SEEN_BY_NOTIFIER = new ArrayList<>();

  @Autowired RegisterParticipant registerParticipant;

  private final JdbcTemplate jdbc = AcceptanceEnvironment.jdbc();

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    AcceptanceEnvironment.register(registry, Map.of("app.json-copy.dir", COPY_DIR.toString()));
  }

  @TestConfiguration
  static class RecordingNotifier {
    @Bean
    @Primary
    RegistrationNotifier recordingNotifier() {
      JdbcTemplate separateConnection = AcceptanceEnvironment.jdbc();
      return registration ->
          ROWS_SEEN_BY_NOTIFIER.add(
              separateConnection.queryForObject(
                  "SELECT COUNT(*) FROM registration WHERE id = ?",
                  Integer.class,
                  registration.id()));
    }
  }

  @BeforeEach
  @AfterEach
  void clean() {
    ROWS_SEEN_BY_NOTIFIER.clear();
    AcceptanceTestBase.clearStorage(jdbc);
    AcceptanceTestBase.clearDirectory(COPY_DIR);
  }

  @Test
  void AC_006_01_notifierRunsAfterTheRegistrationIsCommitted() {
    Registration registration =
        registerParticipant.register(RegistrationTransactionIntegrationTest.submission());

    assertThat(registration.id()).isNotNull();
    assertThat(ROWS_SEEN_BY_NOTIFIER).containsExactly(1);
  }

  @Test
  void AC_006_03_notifierDoesNotRunWhenTheRegistrationIsRolledBack() throws Exception {
    Files.delete(COPY_DIR);
    Files.writeString(COPY_DIR, "not a directory");

    assertThatThrownBy(
        () -> registerParticipant.register(RegistrationTransactionIntegrationTest.submission()));
    assertThat(ROWS_SEEN_BY_NOTIFIER).isEmpty();
  }
}
