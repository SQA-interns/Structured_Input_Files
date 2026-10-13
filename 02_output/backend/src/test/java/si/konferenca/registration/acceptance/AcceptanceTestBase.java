package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base of the acceptance tests that run against the default test settings: the backend on a random
 * port over real HTTP, Testcontainers PostgreSQL and Mailpit, reCAPTCHA test mode.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AcceptanceTestBase {

  @LocalServerPort private int port;

  protected Api api;

  @DynamicPropertySource
  static void backendProperties(DynamicPropertyRegistry registry) {
    AcceptanceEnvironment.register(registry, AcceptanceEnvironment.Settings.defaults());
  }

  @BeforeEach
  void resetState() {
    AcceptanceEnvironment.reset();
    api = new Api(port);
  }

  /**
   * Asserts the "rejected" outcome of `acceptance-criteria.md`: HTTP 400 with an error for the
   * field, no database row, no JSON copy and no email.
   */
  protected static void assertRejected(Api.Response response, String field) {
    assertThat(response.status()).as("status").isEqualTo(400);
    assertThat(response.errorFields()).as("error fields").contains(field);
    assertNothingStored();
  }

  /** No database row, no JSON copy, and no email caught within a short grace period. */
  protected static void assertNothingStored() {
    assertThat(AcceptanceEnvironment.registrationCount()).as("database rows").isZero();
    assertThat(AcceptanceEnvironment.jsonCopies()).as("JSON copies").isEmpty();
    try {
      Thread.sleep(Duration.ofMillis(500));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    assertThat(AcceptanceEnvironment.messages()).as("emails").isEmpty();
  }

  /** Asserts 201 with a registration id and returns the id. */
  protected static String assertAccepted(Api.Response response) {
    assertThat(response.status()).as("status, body: %s", response.text()).isEqualTo(201);
    String id = response.json().get("id").asString();
    assertThat(id).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    return id;
  }
}
