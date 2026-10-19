package si.konferenca.registration.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

/** SR-06: organizer credentials only over HTTPS, except from a loopback address. */
public class OrganizerHttpsOnlyFilter extends OncePerRequestFilter {

  public OrganizerHttpsOnlyFilter(boolean httpsOnly) {}

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
    throw new UnsupportedOperationException("not implemented");
  }
}
