package si.konferenca.registration.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.adapter.web.ApiResponses.CaptchaConfig;
import si.konferenca.registration.adapter.web.ApiResponses.FormConfig;
import si.konferenca.registration.adapter.web.ApiResponses.FormConsent;
import si.konferenca.registration.adapter.web.ApiResponses.FormOption;
import si.konferenca.registration.application.FormSettings;
import si.konferenca.registration.application.GetFormConfiguration;
import si.konferenca.registration.domain.ConferenceCatalogue;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.ConsentDefinition;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.RegistrationType;

/** F-01: the anti-automation mode and site key served to the form (SR-01, SR-02, AR-07). */
class FormConfigControllerTest {

  private static final ConferenceCatalogue CATALOGUE =
      new ConferenceCatalogue(
          List.of(
              new ConferenceOption(
                  "ws",
                  "Workshop",
                  OptionCategory.WORKSHOP,
                  true,
                  EnumSet.of(RegistrationType.STUDENT, RegistrationType.EXTERNAL)),
              new ConferenceOption(
                  "off", "Off", OptionCategory.MEAL, false, EnumSet.of(RegistrationType.STUDENT))),
          List.of(new ConsentDefinition("privacy", "I agree", true)));

  private static FormConfig formConfig(boolean testMode) {
    return new FormConfigController(
            new GetFormConfiguration(CATALOGUE, new FormSettings("Konf", testMode, "site-key")))
        .formConfig();
  }

  @Test
  void productionModeServesTheRecaptchaSiteKey() {
    assertThat(formConfig(false).captcha()).isEqualTo(new CaptchaConfig("recaptcha", "site-key"));
  }

  @Test
  void testModeServesTestModeWithoutAKey() {
    assertThat(formConfig(true).captcha()).isEqualTo(new CaptchaConfig("test", null));
  }

  @Test
  void activeOptionsAndConsentsAreServedInContractShape() {
    FormConfig config = formConfig(false);
    assertThat(config.conferenceName()).isEqualTo("Konf");
    assertThat(config.options())
        .containsExactly(
            new FormOption("ws", "Workshop", "WORKSHOP", List.of("EXTERNAL", "STUDENT")));
    assertThat(config.consents()).containsExactly(new FormConsent("privacy", "I agree", true));
  }
}
