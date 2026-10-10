package si.konferenca.registration.api;

import java.util.Comparator;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import si.konferenca.registration.application.FormConfigService;
import si.konferenca.registration.domain.ConferenceOption;

/** {@code GET /api/form-config} (contract openapi.yaml, AC-003-01). */
@RestController
public class FormConfigController {

  private final FormConfigService service;

  public FormConfigController(FormConfigService service) {
    this.service = service;
  }

  @GetMapping("/api/form-config")
  public FormConfigResponse formConfig() {
    FormConfigService.FormConfig config = service.formConfig();
    return new FormConfigResponse(
        config.conferenceName(),
        config.options().stream().map(FormConfigController::option).toList(),
        config.consents().stream()
            .map(c -> new ConsentResponse(c.id(), c.text(), c.mandatory()))
            .toList(),
        new RecaptchaResponse(
            config.recaptchaSiteKey() == null ? "" : config.recaptchaSiteKey(),
            config.recaptchaTestMode()));
  }

  private static OptionResponse option(ConferenceOption option) {
    List<String> audience =
        option.audience().stream().sorted(Comparator.naturalOrder()).map(Enum::name).toList();
    return new OptionResponse(option.id(), option.name(), option.category().name(), audience);
  }

  /** Response body of the form configuration. */
  public record FormConfigResponse(
      String conferenceName,
      List<OptionResponse> options,
      List<ConsentResponse> consents,
      RecaptchaResponse recaptcha) {}

  /** One active option. */
  public record OptionResponse(String id, String name, String category, List<String> audience) {}

  /** One consent. */
  public record ConsentResponse(String id, String text, boolean mandatory) {}

  /** reCAPTCHA settings for the widget. */
  public record RecaptchaResponse(String siteKey, boolean testMode) {}
}
