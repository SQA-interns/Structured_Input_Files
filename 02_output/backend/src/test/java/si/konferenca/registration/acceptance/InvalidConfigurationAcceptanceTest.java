package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import si.konferenca.registration.RegistrationApplication;

/** US-003: the backend refuses to start with an invalid options configuration. */
class InvalidConfigurationAcceptanceTest {

  @ParameterizedTest(name = "AC-003-03 {0} stops start-up")
  @CsvSource({
    "invalid-duplicate-id.json, ws-dup",
    "invalid-unknown-category.json, PARTY",
    "invalid-missing-name.json, no-name"
  })
  @DisplayName("AC-003-03 invalid options configuration makes the backend refuse to start")
  void ac00303InvalidConfigurationRefused(String file, String problem) {
    Throwable failure = start("classpath:acceptance/" + file);

    assertThat(failure).as("start-up with %s must fail", file).isNotNull();
    assertThat(String.join(" | ", messages(failure)))
        .as("start-up failure names the problem")
        .contains(problem);
  }

  @Test
  @DisplayName("AC-003-03 control: the same start-up with a valid configuration succeeds")
  void ac00303ValidConfigurationStarts() {
    assertThat(start("classpath:acceptance/conference.json")).isNull();
  }

  /** Starts the backend with the given options file; returns the failure, or null if it started. */
  private static Throwable start(String conferenceConfig) {
    Map<String, String> properties =
        AcceptanceEnvironment.properties(
            AcceptanceEnvironment.Settings.defaults().withConferenceConfig(conferenceConfig));
    List<String> args = new ArrayList<>();
    properties.forEach((k, v) -> args.add("--" + k + "=" + v));
    args.add("--server.port=0");
    return catchThrowable(
        () -> {
          try (ConfigurableApplicationContext context =
              new SpringApplicationBuilder(RegistrationApplication.class)
                  .run(args.toArray(String[]::new))) {
            assertThat(context.isActive()).isTrue();
          }
        });
  }

  private static List<String> messages(Throwable failure) {
    List<String> messages = new ArrayList<>();
    for (Throwable t = failure; t != null; t = t.getCause()) {
      messages.add(String.valueOf(t.getMessage()));
    }
    return messages;
  }
}
