package si.konferenca.registration.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import si.konferenca.registration.config.AppProperties;

/**
 * SB-06, SR-03: per client address and endpoint, at most N requests in each fixed one-minute
 * window; excess requests get 429. Failed organizer logins count against the export limit.
 */
@Component
@Order(FilterOrder.RATE_LIMIT)
public class RateLimitFilter extends OncePerRequestFilter {

  static final String MESSAGE = "Too many requests. Please try again in a minute.";
  private static final int MAX_TRACKED_CLIENTS = 10_000;

  private final Map<String, Integer> limits;
  private final Clock clock;
  private final Map<String, Window> windows = new ConcurrentHashMap<>();

  @Autowired
  public RateLimitFilter(AppProperties properties) {
    this(properties, Clock.systemUTC());
  }

  RateLimitFilter(AppProperties properties, Clock clock) {
    AppProperties.Limits l = properties.limits();
    this.limits =
        Map.of(
            "/api/form-config", l.formConfigPerMinute(),
            "/api/registrations", l.registrationsPerMinute(),
            "/api/export", l.exportPerMinute());
    this.clock = clock;
  }

  private record Window(long minute, int count) {}

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !limits.containsKey(request.getRequestURI());
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String endpoint = request.getRequestURI();
    if (!allow(endpoint, request.getRemoteAddr())) {
      response.setHeader("Retry-After", "60");
      ErrorBody.write(response, 429, MESSAGE);
      return;
    }
    chain.doFilter(request, response);
  }

  boolean allow(String endpoint, String client) {
    long minute = clock.millis() / 60_000;
    if (windows.size() > MAX_TRACKED_CLIENTS) {
      windows.values().removeIf(w -> w.minute() < minute);
    }
    Window window =
        windows.compute(
            endpoint + "|" + client,
            (key, current) ->
                current == null || current.minute() != minute
                    ? new Window(minute, 1)
                    : new Window(minute, current.count() + 1));
    return window.count() <= limits.get(endpoint);
  }
}
