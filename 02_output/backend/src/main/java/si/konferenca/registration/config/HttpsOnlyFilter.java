package si.konferenca.registration.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Refuses organizer requests over plain HTTP unless they come from the local machine (SR-06).
 * {@code isSecure()} reflects {@code X-Forwarded-Proto} from trusted proxies.
 */
final class HttpsOnlyFilter extends OncePerRequestFilter {

  private final boolean enabled;

  HttpsOnlyFilter(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (enabled
        && request.getRequestURI().startsWith("/api/export/")
        && !request.isSecure()
        && !loopback(request.getRemoteAddr())) {
      ErrorResponseWriter.write(
          response,
          HttpServletResponse.SC_FORBIDDEN,
          "HTTPS_REQUIRED",
          "Organizer access requires HTTPS.");
      return;
    }
    chain.doFilter(request, response);
  }

  static boolean loopback(String address) {
    if (address == null || !address.matches("[0-9a-fA-F:.]+")) {
      return false;
    }
    try {
      return InetAddress.getByName(address).isLoopbackAddress();
    } catch (UnknownHostException e) {
      return false;
    }
  }
}
