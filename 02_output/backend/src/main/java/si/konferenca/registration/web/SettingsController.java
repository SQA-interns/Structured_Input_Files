package si.konferenca.registration.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import si.konferenca.registration.application.FormSettings;

/** {@code GET /api/settings}: what the form needs; no secret (AR-07). */
@RestController
class SettingsController {

  record ConsentResponse(String id, String text) {}

  record RecaptchaResponse(boolean testMode, String siteKey) {}

  record SettingsResponse(
      String conferenceName, ConsentResponse consent, RecaptchaResponse recaptcha) {}

  private final SettingsResponse settings;

  SettingsController(FormSettings settings) {
    this.settings =
        new SettingsResponse(
            settings.conferenceName(),
            new ConsentResponse(settings.consent().id(), settings.consent().text()),
            new RecaptchaResponse(settings.recaptchaTestMode(), settings.recaptchaSiteKey()));
  }

  @GetMapping("/api/settings")
  SettingsResponse settings() {
    return settings;
  }
}
