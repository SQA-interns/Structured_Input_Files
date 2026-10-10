package si.konferenca.registration.infrastructure;

import si.konferenca.registration.application.CaptchaVerifier;

/**
 * Deterministic reCAPTCHA test mode for the local and test environments only (SR-02): accepts
 * exactly the token {@value #TOKEN}. Production refuses to start with test mode on.
 */
public class TestModeCaptchaVerifier implements CaptchaVerifier {

  public static final String TOKEN = "test-pass";

  @Override
  public boolean verify(String token, String clientIp) {
    return TOKEN.equals(token);
  }
}
