package si.konferenca.registration.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import si.konferenca.registration.config.AppProperties;
import si.konferenca.registration.domain.OptionCatalog;

/** {@code GET /api/settings}: what the form needs; no secret (AR-07). */
@RestController
class SettingsController {

  record ConsentResponse(String id, String text) {}

  record RecaptchaResponse(boolean testMode, String siteKey) {}

  record SettingsResponse(
      String conferenceName, ConsentResponse consent, RecaptchaResponse recaptcha) {}

  private final SettingsResponse settings;

  SettingsController(AppProperties properties, OptionCatalog catalog) {
    AppProperties.Recaptcha recaptcha = properties.recaptcha();
    this.settings =
        new SettingsResponse(
            properties.conferenceName(),
            new ConsentResponse(catalog.consent().id(), catalog.consent().text()),
            new RecaptchaResponse(
                recaptcha.testMode(),
                recaptcha.testMode() || recaptcha.siteKey() == null ? "" : recaptcha.siteKey()));
  }

  @GetMapping("/api/settings")
  SettingsResponse settings() {
    return settings;
  }
}
