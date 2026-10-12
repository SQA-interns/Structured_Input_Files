package si.konferenca.registration.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Refuses organizer requests over plain HTTP from other machines before any credential is checked
 * (SR-06). The scheme comes from the trusted reverse proxy ({@code X-Forwarded-Proto}).
 */
public class OrganizerHttpsFilter extends OncePerRequestFilter {

  private final boolean requireHttps;

  public OrganizerHttpsFilter(boolean requireHttps) {
    this.requireHttps = requireHttps;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (requireHttps
        && request.getRequestURI().startsWith("/api/organizer/")
        && !request.isSecure()
        && !isLoopback(request.getRemoteAddr())) {
      Problems.write(response, HttpStatus.FORBIDDEN);
      return;
    }
    chain.doFilter(request, response);
  }

  private static boolean isLoopback(String address) {
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
