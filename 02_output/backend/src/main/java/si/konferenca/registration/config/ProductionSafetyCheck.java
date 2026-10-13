package si.konferenca.registration.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * With profile {@code prod}, start-up fails when reCAPTCHA test mode is on, a reCAPTCHA key is
 * empty, or organizer HTTPS-only access is off (SR-02, SR-06, spec §3).
 */
@Component
@Profile("prod")
public class ProductionSafetyCheck {

  private final boolean captchaTestMode;
  private final String siteKey;
  private final String secretKey;
  private final boolean organizerHttpsOnly;

  public ProductionSafetyCheck(
      @Value("${app.recaptcha.test-mode}") boolean captchaTestMode,
      @Value("${app.recaptcha.site-key}") String siteKey,
      @Value("${app.recaptcha.secret-key}") String secretKey,
      @Value("${app.organizer.https-only}") boolean organizerHttpsOnly) {
    this.captchaTestMode = captchaTestMode;
    this.siteKey = siteKey;
    this.secretKey = secretKey;
    this.organizerHttpsOnly = organizerHttpsOnly;
    verify();
  }

  private void verify() {
    if (captchaTestMode) {
      throw new IllegalStateException("RECAPTCHA_TEST_MODE must be false in production");
    }
    if (isBlank(siteKey) || isBlank(secretKey)) {
      throw new IllegalStateException(
          "RECAPTCHA_SITE_KEY and RECAPTCHA_SECRET_KEY must be set in production");
    }
    if (!organizerHttpsOnly) {
      throw new IllegalStateException("ORGANIZER_HTTPS_ONLY must be true in production");
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
