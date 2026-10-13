package si.konferenca.registration.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.filter.OncePerRequestFilter;
import si.konferenca.registration.RegistrationProperties;

/** Fixed one-minute windows per client address and endpoint (SR-03, SB-06). */
public class RateLimitFilter extends OncePerRequestFilter {

  static final String MESSAGE = "Too many requests. Please try again later.";
  static final long WINDOW_MILLIS = 60_000;
  static final int CLEANUP_THRESHOLD = 10_000;

  private final RegistrationProperties.RateLimit limits;
  private final Clock clock;
  private final Map<String, Window> windows = new ConcurrentHashMap<>();

  public RateLimitFilter(RegistrationProperties.RateLimit limits, Clock clock) {
    this.limits = limits;
    this.clock = clock;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String bucket = bucket(request.getRequestURI());
    if (bucket == null) {
      chain.doFilter(request, response);
      return;
    }
    long now = clock.millis();
    long window = now / WINDOW_MILLIS;
    if (windows.size() > CLEANUP_THRESHOLD) {
      windows.values().removeIf(w -> w.window() < window);
    }
    Window current =
        windows.merge(
            bucket + "|" + request.getRemoteAddr(),
            new Window(window, 1),
            (old, fresh) -> old.window() == window ? new Window(window, old.count() + 1) : fresh);
    if (current.count() > limit(bucket)) {
      long retryAfter = (WINDOW_MILLIS - now % WINDOW_MILLIS + 999) / 1000;
      response.setHeader("Retry-After", Long.toString(retryAfter));
      ErrorResponses.write(response, 429, MESSAGE);
      return;
    }
    chain.doFilter(request, response);
  }

  static String bucket(String path) {
    if ("/api/form-config".equals(path)) {
      return "form-config";
    }
    if ("/api/registrations".equals(path)) {
      return "registrations";
    }
    if (path.startsWith("/api/organizer/")) {
      return "export";
    }
    return null;
  }

  private int limit(String bucket) {
    return switch (bucket) {
      case "form-config" -> limits.formConfig();
      case "registrations" -> limits.registrations();
      default -> limits.export();
    };
  }

  private record Window(long window, int count) {}
}
