package si.konferenca.registration.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed application settings (specification section 3). Defaults live in {@code
 * application.properties}; secrets have no default (ES-01).
 */
@ConfigurationProperties("app")
public record AppProperties(
    String environment,
    Conference conference,
    Path jsonCopyDir,
    Recaptcha recaptcha,
    Organizer organizer,
    Cors cors,
    RateLimit rateLimit,
    int maxRequestBytes,
    Mail mail) {

  public boolean isProduction() {
    return "production".equals(environment);
  }

  /** Conference name and configuration file (empty file = bundled classpath default). */
  public record Conference(String name, String configFile) {}

  /** reCAPTCHA settings (SR-01, SR-02). */
  public record Recaptcha(boolean testMode, String siteKey, String secretKey, String verifyUrl) {}

  /** Organizer login and notification recipients (BR-08, US-007, SR-06). */
  public record Organizer(String username, String password, String emails, boolean httpsOnly) {}

  /** CORS origin, local only. */
  public record Cors(String allowedOrigin) {}

  /** Requests per client and minute (SR-03). */
  public record RateLimit(
      int registrationPerMinute, int formConfigPerMinute, int exportPerMinute) {}

  /** Sender and transport settings. */
  public record Mail(String from, boolean starttls, boolean smtpAuth) {}
}
