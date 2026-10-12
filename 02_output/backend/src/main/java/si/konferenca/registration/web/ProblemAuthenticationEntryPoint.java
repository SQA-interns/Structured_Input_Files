package si.konferenca.registration.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/** 401 with a Basic challenge and a problem body without details (BR-08, ES-07). */
public class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException e)
      throws IOException {
    response.setHeader("WWW-Authenticate", "Basic realm=\"organizer\"");
    Problems.write(response, HttpStatus.UNAUTHORIZED);
  }
}
