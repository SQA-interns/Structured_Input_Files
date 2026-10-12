package si.konferenca.registration.config;

import java.nio.file.Path;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings that differ between environments (specification section 12, ES-01). */
@ConfigurationProperties("app")
public record AppProperties(
    String environment,
    Conference conference,
    JsonCopy jsonCopy,
    Recaptcha recaptcha,
    Organizer organizer,
    Mail mail,
    Limits limits,
    Cors cors) {

  /** Conference options source and the name shown in emails. */
  public record Conference(String config, String name) {}

  /** Directory of the raw JSON copies. */
  public record JsonCopy(Path dir) {}

  /** reCAPTCHA keys, verification endpoint and test mode (SR-01, SR-02). */
  public record Recaptcha(boolean testMode, String siteKey, String secretKey, String verifyUrl) {}

  /** Organizer credentials, notification recipients and HTTPS-only access (SR-06). */
  public record Organizer(
      String username, String password, List<String> emails, boolean httpsOnly) {

    @Override
    public String toString() {
      return "Organizer[username=" + username + ", emails=" + emails.size() + "]";
    }
  }

  /** Sender address of all emails. */
  public record Mail(String from) {}

  /** Requests per client and minute, and the request body limit (SR-03). */
  public record Limits(
      int formConfigPerMinute, int registrationsPerMinute, int exportPerMinute, int maxBodyBytes) {}

  /** Allowed frontend origin; empty disables CORS. */
  public record Cors(String allowedOrigin) {}
}
