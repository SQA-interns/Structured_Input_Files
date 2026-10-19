package si.konferenca.registration.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import org.springframework.web.filter.OncePerRequestFilter;

/** SR-03, SB-06: fixed-window rate limits per client address and endpoint. */
public class RateLimitFilter extends OncePerRequestFilter {

  public RateLimitFilter(
      Clock clock, int formConfigPerMinute, int registrationPerMinute, int exportPerMinute) {}

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
    throw new UnsupportedOperationException("not implemented");
  }
}
