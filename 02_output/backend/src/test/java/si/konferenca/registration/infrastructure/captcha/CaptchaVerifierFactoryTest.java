package si.konferenca.registration.infrastructure.captcha;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** SR-02: test mode only by configuration; production refuses test mode or empty keys. */
class CaptchaVerifierFactoryTest {

  static final String URL = "https://www.google.com/recaptcha/api/siteverify";

  @Test
  void SR_02_productionWithKeysUsesTheVerifyEndpoint() {
    assertThat(CaptchaVerifierFactory.create("production", false, "site", "secret", URL))
        .isInstanceOf(RecaptchaVerifier.class);
  }

  @Test
  void SR_02_productionRefusesTestMode() {
    assertThatThrownBy(() -> CaptchaVerifierFactory.create("production", true, "s", "k", URL))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("test mode");
  }

  @ParameterizedTest(name = "SR-02 empty keys [{0}]")
  @ValueSource(strings = {"", "  "})
  void SR_02_productionRefusesEmptyKeys(String empty) {
    assertThatThrownBy(() -> CaptchaVerifierFactory.create("production", false, empty, "k", URL))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> CaptchaVerifierFactory.create("production", false, "s", empty, URL))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> CaptchaVerifierFactory.create("production", false, null, null, URL))
        .isInstanceOf(IllegalStateException.class);
  }

  @ParameterizedTest(name = "SR-02 test mode in {0}")
  @ValueSource(strings = {"local", "test"})
  void SR_02_testModeIsAllowedOnlyInLocalAndTest(String environment) {
    assertThat(CaptchaVerifierFactory.create(environment, true, "", "", URL))
        .isInstanceOf(TestModeCaptchaVerifier.class);
  }

  @Test
  void SR_02_withoutTestModeKeysAreRequiredEverywhere() {
    assertThatThrownBy(() -> CaptchaVerifierFactory.create("local", false, "", "", URL))
        .isInstanceOf(IllegalStateException.class);
    assertThat(CaptchaVerifierFactory.create("local", false, "s", "k", URL))
        .isInstanceOf(RecaptchaVerifier.class);
  }

  @ParameterizedTest(name = "SR-02 environment [{0}]")
  @ValueSource(strings = {"", "prod", "staging", "LOCAL"})
  void SR_02_unknownEnvironmentRefusesToStart(String environment) {
    assertThatThrownBy(() -> CaptchaVerifierFactory.create(environment, true, "s", "k", URL))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void SR_02_invalidVerifyUrlRefusesToStart() {
    assertThatThrownBy(() -> CaptchaVerifierFactory.create("production", false, "s", "k", "::"))
        .isInstanceOf(IllegalStateException.class);
  }
}
