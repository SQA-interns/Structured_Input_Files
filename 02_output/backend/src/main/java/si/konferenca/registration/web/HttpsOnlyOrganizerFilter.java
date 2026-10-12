package si.konferenca.registration.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import si.konferenca.registration.config.AppProperties;

/**
 * SR-06: organizer credentials are never accepted over plain HTTP, except from localhost. Runs
 * before the security chain, so credentials sent over plain HTTP are not even evaluated (D-10).
 */
@Component
@Order(FilterOrder.HTTPS_ONLY)
public class HttpsOnlyOrganizerFilter extends OncePerRequestFilter {

  static final String MESSAGE = "The export is only available over HTTPS.";

  private final boolean httpsOnly;

  public HttpsOnlyOrganizerFilter(AppProperties properties) {
    this.httpsOnly = properties.organizer().httpsOnly();
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !"/api/export".equals(request.getRequestURI());
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (httpsOnly && !request.isSecure() && !isLoopback(request.getRemoteAddr())) {
      ErrorBody.write(response, HttpServletResponse.SC_FORBIDDEN, MESSAGE);
      return;
    }
    chain.doFilter(request, response);
  }

  static boolean isLoopback(String address) {
    if (address == null) {
      return false;
    }
    return address.startsWith("127.")
        || "::1".equals(address)
        || "0:0:0:0:0:0:0:1".equals(address)
        || "[::1]".equals(address);
  }
}
