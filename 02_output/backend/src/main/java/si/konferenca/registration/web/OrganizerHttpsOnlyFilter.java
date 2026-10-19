package si.konferenca.registration.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.springframework.web.filter.OncePerRequestFilter;

/** SR-06: organizer credentials only over HTTPS, except from a loopback address. */
public class OrganizerHttpsOnlyFilter extends OncePerRequestFilter {

  static final String EXPORT_PATH = "/api/registrations/export";

  private static final Pattern IPV4_LOOPBACK =
      Pattern.compile("127\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
  private static final Pattern IPV6_LITERAL = Pattern.compile("[0-9a-fA-F:]+");

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
    if (request.isSecure() || isLoopback(request.getRemoteAddr())) {
      chain.doFilter(request, response);
      return;
    }
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType("application/json");
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response
        .getWriter()
        .write("{\"message\":\"Organizer access requires HTTPS.\",\"fieldErrors\":[]}");
  }

  /** Only IP literals are checked, so no name lookup ever happens. */
  static boolean isLoopback(String address) {
    if (address == null || address.isBlank()) {
      return false;
    }
    if (IPV4_LOOPBACK.matcher(address).matches()) {
      return true;
    }
    if (address.contains(":") && IPV6_LITERAL.matcher(address).matches()) {
      try {
        return InetAddress.getByName(address).isLoopbackAddress();
      } catch (UnknownHostException e) {
        return false;
      }
    }
    return false;
  }
}
