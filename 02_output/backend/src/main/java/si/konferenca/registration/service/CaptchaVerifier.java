package si.konferenca.registration.service;

/** Port: verifies a reCAPTCHA response token on the trusted side (SR-01). */
public interface CaptchaVerifier {

  /** True only when the verifier accepts the token. */
  boolean verify(String token, String clientIp);
}
