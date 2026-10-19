package si.konferenca.registration.application;

import si.konferenca.registration.domain.ConferenceConfiguration;

/** Builds the form configuration from the conference configuration (AC-003-01). */
public class FormConfigurationService {

  private final FormConfiguration formConfiguration;

  public FormConfigurationService(ConferenceConfiguration configuration, CaptchaSettings captcha) {
    this.formConfiguration =
        new FormConfiguration(
            configuration.conferenceName(),
            configuration.activeOptions(),
            configuration.consents(),
            captcha);
  }

  public FormConfiguration formConfiguration() {
    return formConfiguration;
  }
}
