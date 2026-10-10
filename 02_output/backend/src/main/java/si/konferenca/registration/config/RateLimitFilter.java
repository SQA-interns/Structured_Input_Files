package si.konferenca.registration.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limits registrations, exports and every other public API call per client address (SR-03, SB-06):
 * 429 with Retry-After.
 */
final class RateLimitFilter extends OncePerRequestFilter {

  private final FixedWindowRateLimiter registrations;
  private final FixedWindowRateLimiter exports;
  private final FixedWindowRateLimiter publicApi;

  RateLimitFilter(
      FixedWindowRateLimiter registrations,
      FixedWindowRateLimiter exports,
      FixedWindowRateLimiter publicApi) {
    this.registrations = registrations;
    this.exports = exports;
    this.publicApi = publicApi;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String path = request.getRequestURI();
    FixedWindowRateLimiter limiter = null;
    if ("POST".equals(request.getMethod()) && "/api/registrations".equals(path)) {
      limiter = registrations;
    } else if (path.startsWith("/api/export/")) {
      limiter = exports;
    } else if (path.startsWith("/api/")) {
      limiter = publicApi;
    }
    if (limiter != null) {
      long retryAfter = limiter.acquire(request.getRemoteAddr());
      if (retryAfter > 0) {
        response.setHeader("Retry-After", Long.toString(retryAfter));
        ErrorResponseWriter.write(
            response, 429, "TOO_MANY_REQUESTS", "Too many requests. Please try again later.");
        return;
      }
    }
    chain.doFilter(request, response);
  }
}
