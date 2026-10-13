package si.konferenca.registration.application;

import org.springframework.stereotype.Service;
import si.konferenca.registration.domain.ConferenceCatalogue;

/** Use case: the form configuration from the catalogue loaded at start-up (US-003). */
@Service
public class GetFormConfiguration {

  private final ConferenceCatalogue catalogue;
  private final FormSettings settings;

  public GetFormConfiguration(ConferenceCatalogue catalogue, FormSettings settings) {
    this.catalogue = catalogue;
    this.settings = settings;
  }

  public FormConfiguration get() {
    return new FormConfiguration(
        settings.conferenceName(),
        catalogue.activeOptions(),
        catalogue.consents(),
        settings.captchaTestMode(),
        settings.captchaTestMode() ? null : settings.siteKey());
  }
}
