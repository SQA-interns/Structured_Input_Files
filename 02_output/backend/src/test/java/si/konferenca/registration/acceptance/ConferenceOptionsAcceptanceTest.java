package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jayway.jsonpath.DocumentContext;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import si.konferenca.registration.RegistrationApplication;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.Api;
import si.konferenca.registration.acceptance.support.TestEnvironment;

/** US-003 Configurable conference options. */
class ConferenceOptionsAcceptanceTest extends AcceptanceTestBase {

  @Test
  @DisplayName("AC-003-01: the form configuration lists only active options with their data")
  void ac_003_01_formConfigurationListsOnlyActiveOptions() {
    Api.Response response = api.get("/api/form-config");

    assertThat(response.status()).as(response.text()).isEqualTo(200);
    DocumentContext json = response.json();
    List<String> ids = json.read("$.options[*].id");
    assertThat(ids)
        .containsExactlyInAnyOrder("ws-ai", "ws-cloud", "ev-dinner", "meal-lunch", "other-tour");
    assertThat(json.<List<String>>read("$.options[?(@.id == 'ev-dinner')].name"))
        .containsExactly("Gala večerja");
    assertThat(json.<List<String>>read("$.options[?(@.id == 'meal-lunch')].category"))
        .containsExactly("MEAL");
    assertThat(json.<List<List<String>>>read("$.options[?(@.id == 'ws-cloud')].availableTo"))
        .containsExactly(List.of("EXTERNAL"));
    assertThat(json.<List<List<String>>>read("$.options[?(@.id == 'ws-ai')].availableTo").get(0))
        .containsExactlyInAnyOrder("EXTERNAL", "STUDENT");
    assertThat(json.<List<Boolean>>read("$.consents[?(@.id == 'privacy')].mandatory"))
        .containsExactly(true);
    assertThat(json.<String>read("$.conferenceName")).isEqualTo(TestEnvironment.CONFERENCE_NAME);
    assertThat(json.<String>read("$.captcha.mode")).isEqualTo("test");
  }

  @Test
  @DisplayName("AC-003-02: a different configuration file changes the offered options")
  void ac_003_02_changedConfigurationIsReflectedWithoutCodeChange() {
    Path alt = TestEnvironment.copyResource("/acceptance/conference-alt.json");

    try (ConfigurableApplicationContext app = start(alt)) {
      int altPort = Integer.parseInt(app.getEnvironment().getProperty("local.server.port"));
      Api.Response response = new Api(altPort).get("/api/form-config");

      assertThat(response.status()).as(response.text()).isEqualTo(200);
      List<String> ids = response.json().read("$.options[*].id");
      assertThat(ids).containsExactlyInAnyOrder("ws-old", "ws-new", "meal-lunch");
    }
  }

  @Test
  @DisplayName("AC-003-03 (control): a valid configuration file starts the backend")
  void ac_003_03_validConfigurationStarts() {
    Path valid = TestEnvironment.copyResource("/acceptance/conference-test.json");

    try (ConfigurableApplicationContext app = start(valid)) {
      assertThat(app.isRunning()).isTrue();
    }
  }

  @ParameterizedTest(name = "AC-003-03: {0} refuses to start")
  @ValueSource(
      strings = {
        "conference-invalid-duplicate-id.json",
        "conference-invalid-unknown-category.json",
        "conference-invalid-missing-name.json"
      })
  void ac_003_03_invalidConfigurationRefusesToStart(String file) {
    Path invalid = TestEnvironment.copyResource("/acceptance/" + file);

    assertThatThrownBy(() -> start(invalid).close()).isInstanceOf(Exception.class);
  }

  private static ConfigurableApplicationContext start(Path conferenceConfig) {
    Map<String, String> props =
        TestEnvironment.baseProperties(
            conferenceConfig, TestEnvironment.newTempDir("json-copies-options-"));
    List<String> args = new ArrayList<>();
    args.add("--server.port=0");
    props.forEach((k, v) -> args.add("--" + k + "=" + v));
    return new SpringApplicationBuilder(RegistrationApplication.class)
        .run(args.toArray(String[]::new));
  }
}
