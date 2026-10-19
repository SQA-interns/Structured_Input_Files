package si.konferenca.registration.infrastructure.captcha;

import si.konferenca.registration.application.CaptchaVerifier;

/** Deterministic test mode (SR-02): only the token test-pass is accepted, no network call. */
public class TestModeCaptchaVerifier implements CaptchaVerifier {

  static final String TEST_TOKEN = "test-pass";

  @Override
  public boolean verify(String token) {
    return TEST_TOKEN.equals(token);
  }
}
