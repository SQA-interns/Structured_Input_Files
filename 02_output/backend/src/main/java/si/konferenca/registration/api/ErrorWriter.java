package si.konferenca.registration.api;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Writes the error shape from servlet filters, where no controller advice applies. */
public final class ErrorWriter {

  private ErrorWriter() {}

  public static void write(HttpServletResponse response, int status, String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/json");
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    String escaped = message.replace("\\", "\\\\").replace("\"", "\\\"");
    response.getWriter().write("{\"message\":\"" + escaped + "\",\"fieldErrors\":[]}");
  }
}
