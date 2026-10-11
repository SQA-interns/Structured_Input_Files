package si.konferenca.registration.adapter.captcha;

import si.konferenca.registration.domain.CaptchaVerifier;

/** Deterministic test mode (test and local only, SR-02): only {@value #TOKEN} passes. */
public final class TestModeCaptchaVerifier implements CaptchaVerifier {

  public static final String TOKEN = "TEST-PASS";

  @Override
  public boolean verify(String token, String clientAddress) {
    return TOKEN.equals(token);
  }
}
