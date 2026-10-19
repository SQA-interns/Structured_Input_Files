package si.konferenca.registration.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.filter.OncePerRequestFilter;

/** SR-03, SB-06: fixed-window rate limits per client address and endpoint. */
public class RateLimitFilter extends OncePerRequestFilter {

  private static final int MAX_TRACKED = 10_000;

  private final Clock clock;
  private final int formConfigPerMinute;
  private final int registrationPerMinute;
  private final int exportPerMinute;
  private final Map<String, Window> windows = new ConcurrentHashMap<>();

  public RateLimitFilter(
      Clock clock, int formConfigPerMinute, int registrationPerMinute, int exportPerMinute) {
    this.clock = clock;
    this.formConfigPerMinute = formConfigPerMinute;
    this.registrationPerMinute = registrationPerMinute;
    this.exportPerMinute = exportPerMinute;
  }

  private record Window(long minute, int count) {}

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/api/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String path = request.getRequestURI();
    int limit = limitFor(path);
    long second = clock.instant().getEpochSecond();
    long minute = second / 60;
    if (windows.size() > MAX_TRACKED) {
      windows.values().removeIf(w -> w.minute() < minute);
    }
    Window window =
        windows.compute(
            path + "|" + request.getRemoteAddr(),
            (key, current) ->
                current == null || current.minute() != minute
                    ? new Window(minute, 1)
                    : new Window(minute, current.count() + 1));
    if (window.count() > limit) {
      response.setStatus(429);
      response.setHeader("Retry-After", String.valueOf(60 - second % 60));
      response.setContentType("application/json");
      response.setCharacterEncoding(StandardCharsets.UTF_8.name());
      response
          .getWriter()
          .write("{\"message\":\"Too many requests. Please try again later.\",\"fieldErrors\":[]}");
      return;
    }
    chain.doFilter(request, response);
  }

  private int limitFor(String path) {
    return switch (path) {
      case "/api/form-config" -> formConfigPerMinute;
      case "/api/registrations" -> registrationPerMinute;
      case OrganizerHttpsOnlyFilter.EXPORT_PATH -> exportPerMinute;
      default -> Math.min(formConfigPerMinute, Math.min(registrationPerMinute, exportPerMinute));
    };
  }
}
