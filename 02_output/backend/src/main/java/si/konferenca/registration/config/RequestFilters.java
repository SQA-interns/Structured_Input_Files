package si.konferenca.registration.config;

import jakarta.servlet.Filter;
import java.util.List;

/** Servlet filters placed in front of authentication, in this order. */
final class RequestFilters {

  private RequestFilters() {}

  static List<Filter> create(AppProperties properties) {
    AppProperties.RateLimits limits = properties.rateLimit();
    return List.of(
        new RateLimitFilter(
            new FixedWindowRateLimiter(
                limits.registration().maxRequests(),
                limits.registration().window(),
                System::nanoTime),
            new FixedWindowRateLimiter(
                limits.export().maxRequests(), limits.export().window(), System::nanoTime)),
        new HttpsOnlyFilter(properties.organizer().httpsOnly()),
        new RequestSizeFilter(properties.request().maxBodyBytes()));
  }
}
