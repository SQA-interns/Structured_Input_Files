package si.konferenca.registration.application;

import si.konferenca.registration.domain.ConferenceConfiguration;

/** Builds the form configuration from the conference configuration (AC-003-01). */
public class FormConfigurationService {

  public FormConfigurationService(ConferenceConfiguration configuration, CaptchaSettings captcha) {}

  public FormConfiguration formConfiguration() {
    throw new UnsupportedOperationException("not implemented");
  }
}
