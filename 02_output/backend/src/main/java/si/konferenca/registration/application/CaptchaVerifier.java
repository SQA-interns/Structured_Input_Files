package si.konferenca.registration.application;

/** Port: reCAPTCHA token verification on the backend (SR-01). */
public interface CaptchaVerifier {

  boolean verify(String token);
}
