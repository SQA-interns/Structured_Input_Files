package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.Payloads;
import si.konferenca.registration.acceptance.support.TestInfrastructure;

/** US-006: the mail server is unavailable (AC-006-03, OQ-03). */
class MailServerDownAcceptanceTest extends AcceptanceTestBase {

  private static final int CLOSED_PORT = TestInfrastructure.closedPort();

  @DynamicPropertySource
  static void unavailableMailServer(DynamicPropertyRegistry registry) {
    TestInfrastructure.register(
        registry,
        Map.of(
            "SMTP_HOST",
            "127.0.0.1",
            "SMTP_PORT",
            CLOSED_PORT,
            "spring.mail.host",
            "127.0.0.1",
            "spring.mail.port",
            CLOSED_PORT));
  }

  @Test
  void AC_006_03_registrationIsAcceptedAndStoredWhenMailServerIsUnavailable() {
    String email = Payloads.uniqueEmail("ac00603");

    HttpResponse<String> response = postRegistration(Payloads.external(email));

    assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
    String id = json(response.body()).path("id").asString();
    assertThat(registrationsWithEmail(email)).isEqualTo(1);
    assertThat(copyFile(id)).exists();
  }
}
