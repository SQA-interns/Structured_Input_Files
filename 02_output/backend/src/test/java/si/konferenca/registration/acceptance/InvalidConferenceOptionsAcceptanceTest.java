package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import si.konferenca.registration.RegistrationApplication;
import si.konferenca.registration.acceptance.support.TestStack;

/** AC-003-03: the backend refuses to start with an invalid options configuration. */
class InvalidConferenceOptionsAcceptanceTest {

  @ParameterizedTest(name = "AC-003-03 configuration {0} prevents startup")
  @ValueSource(
      strings = {
        "invalid-duplicate-id.json",
        "invalid-unknown-category.json",
        "invalid-missing-name.json"
      })
  void ac_003_03_invalidConfigurationPreventsStartup(String file) {
    // The same settings with a valid configuration start, so only the configuration differs.
    try (ConfigurableApplicationContext context =
        start("classpath:acceptance/conference-acceptance.json")) {
      assertThat(context.isRunning()).isTrue();
    }

    assertThatThrownBy(() -> start("classpath:acceptance/" + file).close())
        .isInstanceOf(Exception.class);
  }

  private static ConfigurableApplicationContext start(String configuration) {
    return new SpringApplicationBuilder(RegistrationApplication.class)
        .run(TestStack.commandLine(Map.of("CONFERENCE_CONFIG", configuration)));
  }
}
