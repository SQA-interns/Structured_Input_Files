package si.konferenca.registration.adapter.web;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Writes the generic error body `{"message": ...}` from filters and security handlers. */
final class JsonErrors {

  private JsonErrors() {}

  static void write(HttpServletResponse response, int status, String message) throws IOException {
    response.setStatus(status);
    response.setContentType("application/json");
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.getWriter().write("{\"message\":\"" + escape(message) + "\"}");
  }

  static String escape(String text) {
    StringBuilder out = new StringBuilder(text.length());
    for (char c : text.toCharArray()) {
      if (c == '"' || c == '\\') {
        out.append('\\').append(c);
      } else if (c < 0x20) {
        out.append(String.format("\\u%04x", (int) c));
      } else {
        out.append(c);
      }
    }
    return out.toString();
  }
}
