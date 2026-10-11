package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static si.konferenca.registration.acceptance.support.Payloads.student;

import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import si.konferenca.registration.acceptance.support.AcceptanceTestSupport;
import si.konferenca.registration.acceptance.support.TestEnvironment;

/** US-006: a failing mail server does not lose or reject a registration (D-07). */
class MailServerDownAcceptanceTest extends AcceptanceTestSupport {

  private static final int CLOSED_PORT = TestEnvironment.unusedPort();

  @DynamicPropertySource
  static void unreachableMailServer(DynamicPropertyRegistry registry) {
    Map<String, String> p = TestEnvironment.baseProperties(CONFERENCE_CONFIG, JSON_COPY_DIR);
    p.put("spring.mail.host", "127.0.0.1");
    p.put("spring.mail.port", String.valueOf(CLOSED_PORT));
    p.forEach((key, value) -> registry.add(key, () -> value));
  }

  @Test
  @DisplayName("AC-006-03: with the mail server down the registration is accepted and stored")
  void ac_006_03_registrationIsAcceptedWhenMailServerIsDown() {
    Map<String, Object> submission = student();

    UUID id = registerAccepted(submission);

    assertThat(db.registration(id)).isPresent();
    assertThat(Files.isRegularFile(jsonCopyFile(JSON_COPY_DIR, id))).isTrue();
  }
}
