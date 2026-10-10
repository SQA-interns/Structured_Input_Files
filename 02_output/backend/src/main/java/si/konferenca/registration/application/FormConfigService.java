package si.konferenca.registration.application;

import java.util.List;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.Consent;
import si.konferenca.registration.domain.OptionCatalog;

/** What the registration form needs: active options, consents, reCAPTCHA mode (US-003, AR-07). */
public class FormConfigService {

  private final OptionCatalog catalog;
  private final FormSettings settings;

  public FormConfigService(OptionCatalog catalog, FormSettings settings) {
    this.catalog = catalog;
    this.settings = settings;
  }

  public FormConfig formConfig() {
    return new FormConfig(
        settings.conferenceName(),
        catalog.activeOptions(),
        catalog.consents(),
        settings.recaptchaSiteKey(),
        settings.recaptchaTestMode());
  }

  /** Configuration values the form shows. */
  public record FormSettings(
      String conferenceName, String recaptchaSiteKey, boolean recaptchaTestMode) {}

  /** The form configuration returned to the frontend. */
  public record FormConfig(
      String conferenceName,
      List<ConferenceOption> options,
      List<Consent> consents,
      String recaptchaSiteKey,
      boolean recaptchaTestMode) {}
}
