package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import si.konferenca.registration.RegistrationApplication;
import si.konferenca.registration.acceptance.support.TestInfrastructure;

/** US-003: an invalid options configuration stops the backend from starting (AC-003-03). */
class InvalidConfigurationAcceptanceTest {

  private static String[] args(String conferenceConfig) {
    return TestInfrastructure.asMap(
            Map.of("REGISTRATION_CONFERENCE_CONFIG", conferenceConfig, "server.port", "0"))
        .entrySet()
        .stream()
        .map(e -> "--" + e.getKey() + "=" + e.getValue())
        .toArray(String[]::new);
  }

  private static ConfigurableApplicationContext start(String conferenceConfig) {
    return new SpringApplicationBuilder(RegistrationApplication.class).run(args(conferenceConfig));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "classpath:acceptance/invalid-duplicate-id.json",
        "classpath:acceptance/invalid-unknown-category.json",
        "classpath:acceptance/invalid-missing-name.json"
      })
  void AC_003_03_invalidConfigurationRefusesToStart(String invalidConfig) {
    // Control: the same settings with a valid configuration start, so only the file differs.
    try (ConfigurableApplicationContext valid = start(TestInfrastructure.DEFAULT_CONFIG)) {
      assertThat(valid.isRunning()).isTrue();
      assertThat(valid.getEnvironment().getProperty("REGISTRATION_CONFERENCE_CONFIG"))
          .isEqualTo(TestInfrastructure.DEFAULT_CONFIG);
      assertThat(formConfigServed(valid)).isTrue();
    }

    assertThatThrownBy(() -> start(invalidConfig).close()).isInstanceOf(Exception.class);
  }

  /** The valid application serves the form configuration (so the control is a real backend). */
  private static boolean formConfigServed(ConfigurableApplicationContext context) {
    String port = context.getEnvironment().getProperty("local.server.port");
    try {
      java.net.http.HttpResponse<String> response =
          java.net.http.HttpClient.newHttpClient()
              .send(
                  java.net.http.HttpRequest.newBuilder(
                          java.net.URI.create("http://localhost:" + port + "/api/form-config"))
                      .build(),
                  java.net.http.HttpResponse.BodyHandlers.ofString());
      return response.statusCode() == 200 && response.body().contains("ws-testing");
    } catch (java.io.IOException e) {
      return false;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }
}
