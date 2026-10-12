package si.konferenca.registration.infrastructure.captcha;

import si.konferenca.registration.application.CaptchaVerifier;

/** Deterministic test mode: accepts exactly {@value #TOKEN}, makes no network call (SR-02). */
public class TestModeCaptchaVerifier implements CaptchaVerifier {

  public static final String TOKEN = "test-mode-pass";

  @Override
  public boolean verify(String token, String clientIp) {
    return TOKEN.equals(token);
  }
}
