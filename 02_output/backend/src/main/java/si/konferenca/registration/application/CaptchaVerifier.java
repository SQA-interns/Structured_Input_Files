package si.konferenca.registration.application;

/** Port: anti-automation verification on the server (SR-01). */
public interface CaptchaVerifier {

  boolean verify(String token, String clientIp);
}
