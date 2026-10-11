package si.konferenca.registration.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Refuses organizer requests over plain HTTP unless the client is on localhost (SR-06). Runs before
 * authentication, so credentials sent over HTTP are never checked.
 */
public final class OrganizerTransportGuard extends OncePerRequestFilter {

  private final boolean httpsOnly;

  public OrganizerTransportGuard(boolean httpsOnly) {
    this.httpsOnly = httpsOnly;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/api/export");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (httpsOnly && !request.isSecure() && !isLoopback(request.getRemoteAddr())) {
      ErrorWriter.write(response, HttpServletResponse.SC_FORBIDDEN, "HTTPS is required.");
      return;
    }
    chain.doFilter(request, response);
  }

  static boolean isLoopback(String address) {
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
