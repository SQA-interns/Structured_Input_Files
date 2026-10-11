package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import si.konferenca.registration.RegistrationApplication;
import si.konferenca.registration.acceptance.support.TestEnvironment;

/** SR-02: production refuses to start with reCAPTCHA test mode on or with empty keys. */
class ProductionStartupAcceptanceTest {

  private static Map<String, String> productionProperties() {
    Map<String, String> p =
        TestEnvironment.baseProperties(
            TestEnvironment.copyResource("/acceptance/conference-test.json"),
            TestEnvironment.newTempDir("json-copies-production-"));
    p.put("app.environment", "production");
    p.put("app.recaptcha.test-mode", "false");
    p.put("app.recaptcha.site-key", "production-site-key");
    p.put("app.recaptcha.secret-key", "production-secret-key");
    p.remove("app.organizer.https-only");
    return p;
  }

  private static ConfigurableApplicationContext start(Map<String, String> props) {
    List<String> args = new ArrayList<>();
    args.add("--server.port=0");
    props.forEach((k, v) -> args.add("--" + k + "=" + v));
    return new SpringApplicationBuilder(RegistrationApplication.class)
        .run(args.toArray(String[]::new));
  }

  @Test
  @DisplayName("SR-02 (control): a complete production configuration starts")
  void sr_02_completeProductionConfigurationStarts() {
    try (ConfigurableApplicationContext app = start(productionProperties())) {
      assertThat(app.isRunning()).isTrue();
    }
  }

  @Test
  @DisplayName("SR-02: production with reCAPTCHA test mode on refuses to start")
  void sr_02_productionWithTestModeRefusesToStart() {
    Map<String, String> props = productionProperties();
    props.put("app.recaptcha.test-mode", "true");

    assertThatThrownBy(() -> start(props).close()).isInstanceOf(Exception.class);
  }

  @Test
  @DisplayName("SR-02: production with an empty reCAPTCHA secret key refuses to start")
  void sr_02_productionWithEmptySecretKeyRefusesToStart() {
    Map<String, String> props = productionProperties();
    props.put("app.recaptcha.secret-key", "");

    assertThatThrownBy(() -> start(props).close()).isInstanceOf(Exception.class);
  }

  @Test
  @DisplayName("SR-02: production with an empty reCAPTCHA site key refuses to start")
  void sr_02_productionWithEmptySiteKeyRefusesToStart() {
    Map<String, String> props = productionProperties();
    props.put("app.recaptcha.site-key", "");

    assertThatThrownBy(() -> start(props).close()).isInstanceOf(Exception.class);
  }
}
