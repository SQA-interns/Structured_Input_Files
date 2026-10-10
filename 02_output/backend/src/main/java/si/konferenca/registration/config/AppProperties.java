package si.konferenca.registration.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Every application setting (ES-01); secrets have no default (spec section 3). */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
    @DefaultValue("production") String environment,
    @DefaultValue("Conference") String conferenceName,
    @DefaultValue Mail mail,
    @DefaultValue Organizer organizer,
    @DefaultValue Options options,
    @DefaultValue JsonCopy jsonCopy,
    @DefaultValue Recaptcha recaptcha,
    @DefaultValue Cors cors,
    @DefaultValue RateLimits rateLimit,
    @DefaultValue Request request) {

  public static final String DEFAULT_OPTIONS_LOCATION = "classpath:conference-options.json";

  public boolean production() {
    return "production".equals(environment);
  }

  public record Mail(@DefaultValue("registration@konferenca.si") String sender) {}

  public record Organizer(
      String username,
      String password,
      @DefaultValue List<String> emails,
      @DefaultValue("true") boolean httpsOnly) {}

  public record Options(@DefaultValue(DEFAULT_OPTIONS_LOCATION) String location) {}

  public record JsonCopy(@DefaultValue("/data/json-copies") String directory) {}

  public record Recaptcha(
      @DefaultValue("false") boolean testMode,
      String siteKey,
      String secretKey,
      @DefaultValue("https://www.google.com/recaptcha/api/siteverify") String verifyUrl) {}

  public record Cors(String allowedOrigin) {}

  public record RateLimits(@DefaultValue Limit registration, @DefaultValue ExportLimit export) {}

  /** Registration requests per client and window. */
  public record Limit(
      @DefaultValue("10") int maxRequests, @DefaultValue("PT10M") Duration window) {}

  /** Export requests per client and window. */
  public record ExportLimit(
      @DefaultValue("20") int maxRequests, @DefaultValue("PT10M") Duration window) {}

  public record Request(@DefaultValue("16384") long maxBodyBytes) {}
}
