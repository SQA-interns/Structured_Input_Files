package si.konferenca.registration.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** SR-02: production refuses test mode, empty keys and organizer access without HTTPS. */
class ProductionSafetyCheckTest {

  @Test
  void safeProductionSettingsPass() {
    assertThatCode(() -> new ProductionSafetyCheck(false, "site", "secret", true))
        .doesNotThrowAnyException();
  }

  @Test
  void testModeIsRefused() {
    assertThatThrownBy(() -> new ProductionSafetyCheck(true, "site", "secret", true))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("RECAPTCHA_TEST_MODE");
  }

  @Test
  void emptyOrMissingKeysAreRefused() {
    assertThatThrownBy(() -> new ProductionSafetyCheck(false, " ", "secret", true))
        .hasMessageContaining("RECAPTCHA_SITE_KEY");
    assertThatThrownBy(() -> new ProductionSafetyCheck(false, "site", "", true))
        .hasMessageContaining("RECAPTCHA_SECRET_KEY");
    assertThatThrownBy(() -> new ProductionSafetyCheck(false, null, "secret", true))
        .hasMessageContaining("RECAPTCHA_SITE_KEY");
    assertThatThrownBy(() -> new ProductionSafetyCheck(false, "site", null, true))
        .hasMessageContaining("RECAPTCHA_SECRET_KEY");
  }

  @Test
  void organizerAccessWithoutHttpsIsRefused() {
    assertThatThrownBy(() -> new ProductionSafetyCheck(false, "site", "secret", false))
        .hasMessageContaining("ORGANIZER_HTTPS_ONLY");
  }
}
