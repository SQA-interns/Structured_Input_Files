package si.konferenca.registration.application;

import java.time.Clock;
import si.konferenca.registration.domain.ConferenceConfiguration;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationSubmission;

/** Use case: validate, verify the captcha, store (US-001, US-002, US-005). */
public class RegisterParticipant {

  public RegisterParticipant(
      ConferenceConfiguration configuration,
      CaptchaVerifier captchaVerifier,
      RegistrationStore store,
      Clock clock) {}

  public Registration register(RegistrationSubmission submission) {
    throw new UnsupportedOperationException("not implemented");
  }
}
