package si.konferenca.registration.adapter.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Fixed one-minute window per client IP and endpoint group (SR-03, SB-06, spec §7). The client IP
 * is the address after Tomcat's RemoteIpValve.
 */
public class RateLimitFilter extends OncePerRequestFilter {

  /** Endpoint groups with their own limit. */
  public enum Group {
    FORM_CONFIG,
    REGISTRATIONS,
    EXPORT
  }

  static final int MAX_TRACKED_CLIENTS = 10_000;
  private static final long WINDOW_MILLIS = 60_000;

  private final Map<Group, Integer> limits;
  private final Clock clock;
  private final Map<String, Window> windows = new ConcurrentHashMap<>();

  private record Window(long index, AtomicInteger count) {}

  public RateLimitFilter(Map<Group, Integer> limits, Clock clock) {
    this.limits = new EnumMap<>(limits);
    this.clock = clock;
  }

  static Group group(HttpServletRequest request) {
    return switch (request.getRequestURI()) {
      case "/api/form-config" -> Group.FORM_CONFIG;
      case "/api/registrations" -> Group.REGISTRATIONS;
      case "/api/registrations/export" -> Group.EXPORT;
      default -> null;
    };
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return group(request) == null;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    Group group = group(request);
    long now = clock.millis();
    long index = now / WINDOW_MILLIS;
    if (windows.size() > MAX_TRACKED_CLIENTS) {
      windows.values().removeIf(w -> w.index() < index);
    }
    Window window =
        windows.compute(
            request.getRemoteAddr() + "|" + group,
            (key, old) ->
                old == null || old.index() != index ? new Window(index, new AtomicInteger()) : old);
    if (window.count().incrementAndGet() > limits.get(group)) {
      long retryAfterSeconds = ((index + 1) * WINDOW_MILLIS - now + 999) / 1000;
      response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
      JsonErrors.write(response, 429, "Too many requests. Please try again later.");
      return;
    }
    chain.doFilter(request, response);
  }
}
