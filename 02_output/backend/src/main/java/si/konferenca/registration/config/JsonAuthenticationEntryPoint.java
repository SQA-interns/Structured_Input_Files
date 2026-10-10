package si.konferenca.registration.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/** 401 with {@code WWW-Authenticate: Basic} and the JSON error body (AC-008-02). */
final class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final String realm;

  JsonAuthenticationEntryPoint(String realm) {
    this.realm = realm;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    response.setHeader("WWW-Authenticate", "Basic realm=\"" + realm + "\"");
    ErrorResponseWriter.write(
        response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "Authentication required.");
  }
}
