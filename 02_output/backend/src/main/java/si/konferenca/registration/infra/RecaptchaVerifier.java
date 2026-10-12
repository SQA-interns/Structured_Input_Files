package si.konferenca.registration.infra;

/** Verifies a reCAPTCHA v2 token on the server (SR-01). */
public interface RecaptchaVerifier {

  /** True only when the token is accepted; any error counts as rejected. */
  boolean verify(String token, String clientIp);
}
