package si.konferenca.registration.infrastructure.captcha;

import java.net.URI;
import java.time.Duration;
import java.util.Set;
import si.konferenca.registration.application.CaptchaVerifier;

/** Chooses the verifier for the environment and refuses unsafe settings at startup (SR-02). */
public final class CaptchaVerifierFactory {

  private static final Set<String> ENVIRONMENTS = Set.of("production", "local", "test");
  private static final Duration TIMEOUT = Duration.ofSeconds(5);

  private CaptchaVerifierFactory() {}

  public static CaptchaVerifier create(
      String environment, boolean testMode, String siteKey, String secretKey, String verifyUrl) {
    if (!ENVIRONMENTS.contains(environment)) {
      throw new IllegalStateException(
          "APP_ENVIRONMENT must be production, local or test; the backend refuses to start");
    }
    if (testMode) {
      if ("production".equals(environment)) {
        throw new IllegalStateException(
            "reCAPTCHA test mode is on in production; the backend refuses to start");
      }
      return new TestModeCaptchaVerifier();
    }
    if (isBlank(siteKey) || isBlank(secretKey)) {
      throw new IllegalStateException(
          "reCAPTCHA keys are empty and test mode is off; the backend refuses to start");
    }
    URI url;
    try {
      url = URI.create(verifyUrl);
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("RECAPTCHA_VERIFY_URL is not a valid URL", e);
    }
    if (url.getScheme() == null || url.getHost() == null) {
      throw new IllegalStateException("RECAPTCHA_VERIFY_URL is not a valid URL");
    }
    return new RecaptchaVerifier(url, secretKey, TIMEOUT);
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
