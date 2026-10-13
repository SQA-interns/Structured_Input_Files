package si.konferenca.registration.adapter.web;

import java.util.Arrays;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import si.konferenca.registration.adapter.web.ApiResponses.CaptchaConfig;
import si.konferenca.registration.adapter.web.ApiResponses.FormConfig;
import si.konferenca.registration.adapter.web.ApiResponses.FormConsent;
import si.konferenca.registration.adapter.web.ApiResponses.FormOption;
import si.konferenca.registration.application.FormConfiguration;
import si.konferenca.registration.application.GetFormConfiguration;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.RegistrationType;

/** `GET /api/form-config` (US-003, spec §5). */
@RestController
public class FormConfigController {

  private final GetFormConfiguration getFormConfiguration;

  public FormConfigController(GetFormConfiguration getFormConfiguration) {
    this.getFormConfiguration = getFormConfiguration;
  }

  @GetMapping("/api/form-config")
  public FormConfig formConfig() {
    FormConfiguration config = getFormConfiguration.get();
    return new FormConfig(
        config.conferenceName(),
        config.activeOptions().stream().map(FormConfigController::option).toList(),
        config.consents().stream()
            .map(c -> new FormConsent(c.id(), c.text(), c.mandatory()))
            .toList(),
        config.captchaTestMode()
            ? new CaptchaConfig("test", null)
            : new CaptchaConfig("recaptcha", config.siteKey()));
  }

  private static FormOption option(ConferenceOption option) {
    return new FormOption(
        option.id(),
        option.name(),
        option.category().name(),
        Arrays.stream(RegistrationType.values())
            .filter(option.availableTo()::contains)
            .map(Enum::name)
            .toList());
  }
}
