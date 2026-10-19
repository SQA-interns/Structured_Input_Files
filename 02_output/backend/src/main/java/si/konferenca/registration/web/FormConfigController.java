package si.konferenca.registration.web;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import si.konferenca.registration.application.FormConfiguration;
import si.konferenca.registration.application.FormConfigurationService;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.RegistrationType;

/** GET /api/form-config (openapi.json getFormConfig). */
@RestController
public class FormConfigController {

  private final FormConfigurationService service;

  public FormConfigController(FormConfigurationService service) {
    this.service = service;
  }

  @GetMapping("/api/form-config")
  public FormConfigResponse formConfig() {
    FormConfiguration config = service.formConfiguration();
    return new FormConfigResponse(
        config.conferenceName(),
        config.options().stream()
            .map(
                o ->
                    new OptionResponse(
                        o.id(), o.name(), o.category(), o.availableTo().stream().sorted().toList()))
            .toList(),
        config.consents().stream()
            .map(c -> new ConsentResponse(c.id(), c.text(), c.mandatory()))
            .toList(),
        new RecaptchaResponse(config.captcha().siteKey(), config.captcha().testMode()));
  }

  /** openapi.json FormConfig. */
  public record FormConfigResponse(
      String conferenceName,
      List<OptionResponse> options,
      List<ConsentResponse> consents,
      RecaptchaResponse recaptcha) {}

  /** openapi.json Option. */
  public record OptionResponse(
      String id, String name, OptionCategory category, List<RegistrationType> availableTo) {}

  /** openapi.json Consent. */
  public record ConsentResponse(String id, String text, boolean mandatory) {}

  /** openapi.json FormConfig.recaptcha. */
  public record RecaptchaResponse(String siteKey, boolean testMode) {}
}
