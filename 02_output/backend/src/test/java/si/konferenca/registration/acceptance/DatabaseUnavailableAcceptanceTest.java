package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import si.konferenca.registration.acceptance.support.AcIdDisplayNames;
import si.konferenca.registration.acceptance.support.AcceptanceSupport;
import si.konferenca.registration.acceptance.support.Mailpit;
import si.konferenca.registration.acceptance.support.TestEnvironment;

/** AC-005-03: the database becomes unavailable; a dedicated database is stopped. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayNameGeneration(AcIdDisplayNames.class)
class DatabaseUnavailableAcceptanceTest extends AcceptanceSupport {

  private static final PostgreSQLContainer OWN_DATABASE =
      new PostgreSQLContainer(DockerImageName.parse(TestEnvironment.POSTGRES_IMAGE));
  private static final Path OWN_JSON_DIR = TestEnvironment.createTempDirectory("json-db-down");

  static {
    OWN_DATABASE.start();
  }

  @LocalServerPort private int port;

  @DynamicPropertySource
  static void environment(DynamicPropertyRegistry registry) {
    Map<String, Supplier<Object>> overrides =
        Map.of(
            "spring.datasource.url", OWN_DATABASE::getJdbcUrl,
            "spring.datasource.username", OWN_DATABASE::getUsername,
            "spring.datasource.password", OWN_DATABASE::getPassword,
            "spring.datasource.hikari.connection-timeout", () -> "3000",
            "app.json-copy.directory", OWN_JSON_DIR::toString);
    TestEnvironment.register(registry, overrides);
  }

  @Override
  protected int port() {
    return port;
  }

  @Test
  void ac_005_03_unavailableDatabaseMeansNoJsonCopyNoEmailAndAnErrorWithoutInternals() {
    String email = uniqueEmail("ac-005-03");
    long copiesBefore = jsonCopyCount(OWN_JSON_DIR);
    OWN_DATABASE.stop();

    HttpResponse<String> response = register(validExternal(email));

    assertThat(response.statusCode()).isEqualTo(500);
    assertThat(json(response).path("error").asString()).isEqualTo("INTERNAL_ERROR");
    assertNoInternals(response.body());
    assertThat(jsonCopyCount(OWN_JSON_DIR)).isEqualTo(copiesBefore);
    Mailpit mailpit = TestEnvironment.mailpit();
    mailpit.assertNoMessageTo(email, Duration.ofSeconds(3));
  }
}
