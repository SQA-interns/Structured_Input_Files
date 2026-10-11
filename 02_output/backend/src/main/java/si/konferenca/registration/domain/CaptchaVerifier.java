package si.konferenca.registration.domain;

/** Port: anti-automation check of a captcha token (SR-01). */
public interface CaptchaVerifier {

  /**
   * True when the token is accepted, false when it is rejected.
   *
   * @throws CaptchaUnavailableException when the verifier cannot answer
   */
  boolean verify(String token, String clientAddress);
}
