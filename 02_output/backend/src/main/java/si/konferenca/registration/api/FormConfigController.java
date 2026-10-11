package si.konferenca.registration.api;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import si.konferenca.registration.domain.ConferenceCatalog;
import si.konferenca.registration.domain.FormSettings;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.RegistrationType;

/** GET /api/form-config: what the registration form shows (AC-003-01, AC-003-02). */
@RestController
public class FormConfigController {

  private final ConferenceCatalog catalog;
  private final FormSettings settings;

  public FormConfigController(ConferenceCatalog catalog, FormSettings settings) {
    this.catalog = catalog;
    this.settings = settings;
  }

  @GetMapping("/api/form-config")
  public FormConfig formConfig() {
    List<OptionDto> options =
        catalog.activeOptions().stream()
            .map(
                o ->
                    new OptionDto(
                        o.id(), o.name(), o.category(), o.availableTo().stream().sorted().toList()))
            .toList();
    List<ConsentDto> consents =
        catalog.consents().stream()
            .map(c -> new ConsentDto(c.id(), c.text(), c.mandatory()))
            .toList();
    Captcha captcha =
        settings.captchaTestMode()
            ? new Captcha("test", "")
            : new Captcha("recaptcha", settings.captchaSiteKey());
    return new FormConfig(settings.conferenceName(), captcha, consents, options);
  }

  /** Response body (openapi.json FormConfig). */
  public record FormConfig(
      String conferenceName, Captcha captcha, List<ConsentDto> consents, List<OptionDto> options) {}

  /** Captcha mode and site key. */
  public record Captcha(String mode, String siteKey) {}

  /** A consent as shown on the form. */
  public record ConsentDto(String id, String text, boolean mandatory) {}

  /** An active option as shown on the form. */
  public record OptionDto(
      String id, String name, OptionCategory category, List<RegistrationType> availableTo) {}
}
