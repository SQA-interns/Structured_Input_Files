package si.konferenca.registration.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import tools.jackson.databind.json.JsonMapper;

/** The Error schema of openapi.json: a generic message and optional field errors. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorBody(String message, List<FieldErrorBody> fieldErrors) {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  /** One field error. */
  public record FieldErrorBody(String field, String message) {}

  public static ErrorBody of(String message) {
    return new ErrorBody(message, null);
  }

  /** Writes the body to a servlet response, for errors raised outside of controllers. */
  static void write(jakarta.servlet.http.HttpServletResponse response, int status, String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/json");
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setHeader("X-Content-Type-Options", "nosniff");
    response.setHeader("Cache-Control", "no-store");
    response.getOutputStream().write(JSON.writeValueAsBytes(of(message)));
  }
}
