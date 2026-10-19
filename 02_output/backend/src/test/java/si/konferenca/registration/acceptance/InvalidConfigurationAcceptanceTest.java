package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import si.konferenca.registration.RegistrationApplication;
import si.konferenca.registration.acceptance.support.AcceptanceEnvironment;

/**
 * US-003: the backend refuses to start with an invalid options configuration; the startup error
 * names the configuration (conference-config.schema.json).
 */
class InvalidConfigurationAcceptanceTest {

  @ParameterizedTest(name = "AC-003-03 {0}")
  @ValueSource(
      strings = {
        "invalid-duplicate-id.json",
        "invalid-unknown-category.json",
        "invalid-missing-name.json"
      })
  void AC_003_03_backendRefusesToStartWithInvalidOptionsConfiguration(String file) {
    String[] args =
        AcceptanceEnvironment.asArguments(
            Map.of(
                "app.conference.config-file",
                AcceptanceEnvironment.resource(file).toString(),
                "app.json-copy.dir",
                AcceptanceEnvironment.newTempDir("json-copies-invalid").toString()));

    Throwable failure = null;
    try {
      ConfigurableApplicationContext context =
          SpringApplication.run(RegistrationApplication.class, args);
      context.close();
    } catch (Throwable e) {
      failure = e;
    }

    if (failure == null) {
      fail("the backend started with " + file);
    }
    assertThat(hasConfigurationCause(failure))
        .as("cause chain names the invalid conference configuration")
        .isTrue();
  }

  private static boolean hasConfigurationCause(Throwable failure) {
    for (Throwable t = failure; t != null; t = t.getCause()) {
      if (t.getMessage() != null && t.getMessage().contains("Invalid conference configuration:")) {
        return true;
      }
      if (t.getCause() == t) {
        break;
      }
    }
    return false;
  }
}
