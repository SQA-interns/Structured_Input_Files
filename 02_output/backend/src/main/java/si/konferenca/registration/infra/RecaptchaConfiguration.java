package si.konferenca.registration.infra;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import si.konferenca.registration.config.AppProperties;

/** Chooses the verifier: test mode only when configured (SR-02, guarded by StartupGuard). */
@Configuration
public class RecaptchaConfiguration {

  @Bean
  RecaptchaVerifier recaptchaVerifier(AppProperties properties) {
    AppProperties.Recaptcha recaptcha = properties.recaptcha();
    if (recaptcha.testMode()) {
      return new TestModeRecaptchaVerifier();
    }
    return new GoogleRecaptchaVerifier(recaptcha.verifyUrl(), recaptcha.secretKey());
  }
}
