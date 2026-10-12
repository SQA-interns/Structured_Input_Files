package si.konferenca.registration.web;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import si.konferenca.registration.conference.ConferenceConfiguration;
import si.konferenca.registration.conference.ConferenceOption;
import si.konferenca.registration.conference.ConsentDefinition;
import si.konferenca.registration.config.AppProperties;

/** GET /api/form-config (AC-003-01): active options, consents and the reCAPTCHA site key. */
@RestController
public class FormConfigController {

  private final ConferenceConfiguration conference;
  private final AppProperties properties;

  public FormConfigController(ConferenceConfiguration conference, AppProperties properties) {
    this.conference = conference;
    this.properties = properties;
  }

  /** The form configuration of openapi.json (FormConfig). */
  public record FormConfig(
      String conferenceName,
      Recaptcha recaptcha,
      List<Option> options,
      List<ConsentDefinition> consents) {}

  /** The public reCAPTCHA settings; the site key is the only key the frontend gets (AR-07). */
  public record Recaptcha(String siteKey, boolean testMode) {}

  /** An active option. */
  public record Option(String id, String name, String category, List<String> availableTo) {

    static Option of(ConferenceOption option) {
      return new Option(
          option.id(),
          option.name(),
          option.category().name(),
          option.availableTo().stream().map(Enum::name).sorted().toList());
    }
  }

  @GetMapping("/api/form-config")
  public FormConfig formConfig() {
    AppProperties.Recaptcha recaptcha = properties.recaptcha();
    return new FormConfig(
        properties.conference().name(),
        new Recaptcha(recaptcha.siteKey() == null ? "" : recaptcha.siteKey(), recaptcha.testMode()),
        conference.activeOptions().stream().map(Option::of).toList(),
        conference.consents());
  }
}
