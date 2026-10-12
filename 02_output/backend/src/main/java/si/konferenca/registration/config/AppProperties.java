package si.konferenca.registration.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Application settings, bound from the environment variables of the specification (S11). */
@ConfigurationProperties("app")
public record AppProperties(
    String conferenceName,
    String optionsLocation,
    String jsonCopyDir,
    Mail mail,
    Organizer organizer,
    Recaptcha recaptcha,
    List<String> corsAllowedOrigins,
    RateLimit rateLimit,
    long maxRequestBytes) {

  public AppProperties {
    corsAllowedOrigins = corsAllowedOrigins == null ? List.of() : List.copyOf(corsAllowedOrigins);
  }

  /** Sender settings. */
  public record Mail(String from) {}

  /** Organizer login and notification recipients. */
  public record Organizer(
      String username, String password, List<String> emails, boolean requireHttps) {

    public Organizer {
      emails = emails == null ? List.of() : List.copyOf(emails);
    }
  }

  /** Anti-automation settings (SR-01, SR-02). */
  public record Recaptcha(boolean testMode, String siteKey, String secretKey, String verifyUrl) {}

  /** Requests per client IP and minute (SR-03, SB-06). */
  public record RateLimit(int registrationsPerMinute, int readsPerMinute, int organizerPerMinute) {}
}
