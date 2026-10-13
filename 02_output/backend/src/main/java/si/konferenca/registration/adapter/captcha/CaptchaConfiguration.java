package si.konferenca.registration.adapter.captcha;

import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import si.konferenca.registration.application.Ports.CaptchaVerifier;

/** Chooses the verifier: test mode only when RECAPTCHA_TEST_MODE is true (SR-02). */
@Configuration
public class CaptchaConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(CaptchaConfiguration.class);

  @Bean
  public CaptchaVerifier captchaVerifier(
      @Value("${app.recaptcha.test-mode}") boolean testMode,
      @Value("${app.recaptcha.secret-key}") String secretKey,
      @Value("${app.recaptcha.verify-url}") URI verifyUrl) {
    if (testMode) {
      LOG.warn("reCAPTCHA test mode is ON: only for the test and local environments");
      return new TestModeCaptchaVerifier();
    }
    return new GoogleCaptchaVerifier(verifyUrl, secretKey);
  }
}
