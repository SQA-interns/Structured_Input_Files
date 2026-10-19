package si.konferenca.registration.infrastructure.captcha;

import si.konferenca.registration.application.CaptchaVerifier;

/** Chooses the verifier for the environment and refuses unsafe settings at startup (SR-02). */
public final class CaptchaVerifierFactory {

  private CaptchaVerifierFactory() {}

  public static CaptchaVerifier create(
      String environment, boolean testMode, String siteKey, String secretKey, String verifyUrl) {
    throw new UnsupportedOperationException("not implemented");
  }
}
