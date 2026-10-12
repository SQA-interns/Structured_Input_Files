package si.konferenca.registration.web;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;

/** Writes a minimal {@code application/problem+json} body from filters and entry points. */
final class Problems {

  private Problems() {}

  static void write(HttpServletResponse response, HttpStatus status) throws IOException {
    response.setStatus(status.value());
    response.setContentType("application/problem+json");
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response
        .getWriter()
        .write(
            "{\"type\":\"about:blank\",\"title\":\""
                + status.getReasonPhrase()
                + "\",\"status\":"
                + status.value()
                + "}");
  }
}
