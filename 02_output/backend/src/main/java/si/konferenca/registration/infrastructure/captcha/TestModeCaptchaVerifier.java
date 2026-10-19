package si.konferenca.registration.infrastructure.captcha;

import si.konferenca.registration.application.CaptchaVerifier;

/** Deterministic test mode (SR-02): only the token test-pass is accepted, no network call. */
public class TestModeCaptchaVerifier implements CaptchaVerifier {

  @Override
  public boolean verify(String token) {
    throw new UnsupportedOperationException("not implemented");
  }
}
