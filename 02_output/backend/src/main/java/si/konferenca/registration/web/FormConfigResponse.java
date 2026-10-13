package si.konferenca.registration.web;

import java.util.List;
import si.konferenca.registration.domain.ConferenceCatalog;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.ConsentDefinition;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.RegistrationType;

/** OpenAPI schema FormConfig: active options only (AC-003-01). */
public record FormConfigResponse(
    String conferenceName,
    String recaptchaSiteKey,
    boolean recaptchaTestMode,
    List<Consent> consents,
    List<Option> options) {

  /** A consent as shown on the form. */
  public record Consent(String id, String text, boolean mandatory) {}

  /** An active option with the registration types it is available to. */
  public record Option(
      String id, String name, OptionCategory category, List<RegistrationType> availableTo) {}

  static FormConfigResponse of(
      String conferenceName, String siteKey, boolean testMode, ConferenceCatalog catalog) {
    List<Consent> consents = catalog.consents().stream().map(FormConfigResponse::consent).toList();
    List<Option> options =
        catalog.activeOptions().stream().map(FormConfigResponse::option).toList();
    return new FormConfigResponse(
        conferenceName, testMode ? "" : siteKey, testMode, consents, options);
  }

  private static Consent consent(ConsentDefinition consent) {
    return new Consent(consent.id(), consent.text(), consent.mandatory());
  }

  private static Option option(ConferenceOption option) {
    List<RegistrationType> types = option.availableTo().stream().sorted().toList();
    return new Option(option.id(), option.name(), option.category(), types);
  }
}
