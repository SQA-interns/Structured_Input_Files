package si.konferenca.registration.application;

/** Port: verifies a reCAPTCHA token on the trusted side (SR-01). */
public interface CaptchaVerifier {

  /** True only when the token is accepted; any failure counts as a rejection. */
  boolean verify(String token, String clientIp);
}
