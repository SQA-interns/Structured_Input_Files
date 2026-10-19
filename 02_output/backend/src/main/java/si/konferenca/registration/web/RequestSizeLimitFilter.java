package si.konferenca.registration.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

/** SR-03: request bodies larger than the limit are refused with 413. */
public class RequestSizeLimitFilter extends OncePerRequestFilter {

  public RequestSizeLimitFilter(long maxBytes) {}

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
    throw new UnsupportedOperationException("not implemented");
  }
}
