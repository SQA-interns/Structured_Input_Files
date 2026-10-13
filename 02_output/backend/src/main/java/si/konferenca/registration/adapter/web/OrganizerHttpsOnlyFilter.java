package si.konferenca.registration.adapter.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Refuses organizer access over plain HTTP from a client that is not on localhost, before any
 * credential is evaluated (SR-06, AC-008-04, D-10). The scheme and client address are those set by
 * Tomcat's RemoteIpValve from a trusted proxy's forwarded headers.
 */
public class OrganizerHttpsOnlyFilter extends OncePerRequestFilter {

  static final String EXPORT_PATH = "/api/registrations/export";

  private static final Pattern IP_LITERAL =
      Pattern.compile("^(\\d{1,3}(\\.\\d{1,3}){3}|\\[?[0-9a-fA-F]*:[0-9a-fA-F:.]*(%[\\w.]+)?]?)$");

  private final boolean httpsOnly;

  public OrganizerHttpsOnlyFilter(boolean httpsOnly) {
    this.httpsOnly = httpsOnly;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !httpsOnly || !EXPORT_PATH.equals(request.getRequestURI());
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (!request.isSecure() && !isLoopback(request.getRemoteAddr())) {
      JsonErrors.write(response, 403, "Organizer access requires HTTPS.");
      return;
    }
    chain.doFilter(request, response);
  }

  static boolean isLoopback(String address) {
    // Only IP literals are resolved, so InetAddress never performs a name lookup here.
    if (address == null || !IP_LITERAL.matcher(address).matches()) {
      return false;
    }
    try {
      return InetAddress.getByName(address).isLoopbackAddress();
    } catch (UnknownHostException e) {
      return false;
    }
  }
}
