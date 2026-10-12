package si.konferenca.registration.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Fixed one-minute window per client IP and endpoint group (SR-03, SB-06, D-19). Organizer requests
 * count before authentication, so failed logins are limited too.
 */
public class RateLimitFilter extends OncePerRequestFilter {

  private static final long WINDOW_MILLIS = 60_000;
  private static final int MAX_TRACKED = 10_000;

  /** Requests allowed per minute and client IP. */
  public record Limits(int registrations, int reads, int organizer) {}

  private record Window(long start, int count) {}

  private final Limits limits;
  private final Clock clock;
  private final Map<String, Window> windows = new ConcurrentHashMap<>();

  public RateLimitFilter(Limits limits, Clock clock) {
    this.limits = limits;
    this.clock = clock;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String path = request.getRequestURI();
    String group;
    int limit;
    if (path.startsWith("/api/organizer/")) {
      group = "organizer";
      limit = limits.organizer();
    } else if ("/api/registrations".equals(path) && "POST".equals(request.getMethod())) {
      group = "registrations";
      limit = limits.registrations();
    } else if (path.startsWith("/api/")) {
      group = "reads";
      limit = limits.reads();
    } else {
      chain.doFilter(request, response);
      return;
    }
    long now = clock.millis();
    if (windows.size() > MAX_TRACKED) {
      windows.values().removeIf(w -> now - w.start() >= WINDOW_MILLIS);
    }
    Window window =
        windows.merge(
            group + '|' + request.getRemoteAddr(),
            new Window(now, 1),
            (old, fresh) ->
                now - old.start() >= WINDOW_MILLIS
                    ? fresh
                    : new Window(old.start(), old.count() + 1));
    if (window.count() > limit) {
      long retryAfter = Math.max(1, (window.start() + WINDOW_MILLIS - now + 999) / 1000);
      response.setHeader("Retry-After", Long.toString(retryAfter));
      Problems.write(response, HttpStatus.TOO_MANY_REQUESTS);
      return;
    }
    chain.doFilter(request, response);
  }
}
