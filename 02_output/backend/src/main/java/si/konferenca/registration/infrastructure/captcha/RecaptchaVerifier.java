package si.konferenca.registration.infrastructure.captcha;

import java.net.URI;
import java.time.Duration;
import si.konferenca.registration.application.CaptchaVerifier;

/** Production path (SR-01): verifies the token with the reCAPTCHA verify endpoint. */
public class RecaptchaVerifier implements CaptchaVerifier {

  public RecaptchaVerifier(URI verifyUrl, String secretKey, Duration timeout) {}

  @Override
  public boolean verify(String token) {
    throw new UnsupportedOperationException("not implemented");
  }
}
