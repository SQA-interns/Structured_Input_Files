package si.konferenca.registration;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings under {@code registration.*}; application.properties maps each from its environment
 * variable (spec section 13).
 */
@ConfigurationProperties("registration")
public record RegistrationProperties(
    String conferenceName,
    String conferenceConfig,
    Path jsonCopyDir,
    Mail mail,
    Recaptcha recaptcha,
    Organizer organizer,
    Cors cors,
    RateLimit rateLimit,
    long maxBodyBytes) {

  /** Sender and organizer notification recipients. */
  public record Mail(String from, List<String> organizerRecipients) {

    public Mail {
      organizerRecipients =
          organizerRecipients == null
              ? List.of()
              : organizerRecipients.stream().map(String::strip).filter(s -> !s.isEmpty()).toList();
    }
  }

  /** reCAPTCHA v2 settings (SR-01, SR-02). */
  public record Recaptcha(boolean testMode, String siteKey, String secretKey, URI verifyUrl) {

    @Override
    public String toString() {
      return "Recaptcha[testMode=" + testMode + ", verifyUrl=" + verifyUrl + "]";
    }
  }

  /** Organizer login and transport rule (BR-08, SR-06). */
  public record Organizer(String username, String password, boolean httpsOnly) {

    @Override
    public String toString() {
      return "Organizer[httpsOnly=" + httpsOnly + "]";
    }
  }

  /** Cross-origin access, local development only. */
  public record Cors(List<String> allowedOrigins) {

    public Cors {
      allowedOrigins =
          allowedOrigins == null
              ? List.of()
              : allowedOrigins.stream().map(String::strip).filter(s -> !s.isEmpty()).toList();
    }
  }

  /** Requests per client and minute (SR-03). */
  public record RateLimit(int formConfig, int registrations, int export) {}
}
