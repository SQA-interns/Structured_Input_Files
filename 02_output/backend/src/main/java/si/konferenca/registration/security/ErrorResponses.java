package si.konferenca.registration.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/** Writes the error model of spec 4.3 from filters, which run before Spring MVC. */
final class ErrorResponses {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private ErrorResponses() {}

  static void write(HttpServletResponse response, int status, String message) throws IOException {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("message", message);
    body.put("fieldErrors", Map.of());
    response.setStatus(status);
    response.setContentType("application/json");
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.getOutputStream().write(JSON.writeValueAsBytes(body));
  }
}
