package si.konferenca.registration.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Refuses organizer requests over plain HTTP from clients that are not on localhost, before any
 * credentials are evaluated (SR-06, AC-008-04). Scheme and client address come from the trusted
 * proxy headers (server.forward-headers-strategy=native).
 */
public class HttpsOnlyFilter extends OncePerRequestFilter {

  static final String ORGANIZER_PATH = "/api/organizer/";
  static final String MESSAGE = "HTTPS is required.";

  private final boolean httpsOnly;

  public HttpsOnlyFilter(boolean httpsOnly) {
    this.httpsOnly = httpsOnly;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (httpsOnly
        && request.getRequestURI().startsWith(ORGANIZER_PATH)
        && !request.isSecure()
        && !isLoopback(request.getRemoteAddr())) {
      ErrorResponses.write(response, HttpServletResponse.SC_FORBIDDEN, MESSAGE);
      return;
    }
    chain.doFilter(request, response);
  }

  static boolean isLoopback(String address) {
    if (address == null || address.isBlank()) {
      return false;
    }
    try {
      // Literal addresses only: getByName does not resolve names for IP literals.
      return isIpLiteral(address) && InetAddress.getByName(address).isLoopbackAddress();
    } catch (UnknownHostException e) {
      return false;
    }
  }

  private static boolean isIpLiteral(String address) {
    return address.contains(":") || address.chars().allMatch(c -> c == '.' || Character.isDigit(c));
  }
}
