package si.konferenca.registration.adapter.captcha;

import si.konferenca.registration.application.Ports.CaptchaVerifier;

/**
 * Deterministic verifier for the test and local environments (SR-02): only the token {@value
 * #PASS_TOKEN} is accepted; no call leaves the machine.
 */
public class TestModeCaptchaVerifier implements CaptchaVerifier {

  public static final String PASS_TOKEN = "test-pass";

  @Override
  public boolean verify(String token, String clientIp) {
    return PASS_TOKEN.equals(token);
  }
}
