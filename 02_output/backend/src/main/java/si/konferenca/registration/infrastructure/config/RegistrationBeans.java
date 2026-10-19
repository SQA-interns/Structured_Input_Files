package si.konferenca.registration.infrastructure.config;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import si.konferenca.registration.application.CaptchaVerifier;
import si.konferenca.registration.application.RegisterParticipant;
import si.konferenca.registration.application.RegistrationStore;
import si.konferenca.registration.domain.ConferenceConfiguration;
import si.konferenca.registration.infrastructure.captcha.TestModeCaptchaVerifier;

/** Wires the registration use case to its ports. */
@Configuration
public class RegistrationBeans {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  CaptchaVerifier captchaVerifier(@Value("${app.recaptcha.test-mode}") boolean testMode) {
    if (!testMode) {
      throw new IllegalStateException("reCAPTCHA verification is not configured");
    }
    return new TestModeCaptchaVerifier();
  }

  @Bean
  RegisterParticipant registerParticipant(
      ConferenceConfiguration configuration,
      CaptchaVerifier captchaVerifier,
      RegistrationStore store,
      Clock clock) {
    return new RegisterParticipant(configuration, captchaVerifier, store, clock);
  }
}
