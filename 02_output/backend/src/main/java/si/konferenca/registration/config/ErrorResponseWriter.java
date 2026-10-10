package si.konferenca.registration.config;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import si.konferenca.registration.api.ApiError;
import tools.jackson.databind.json.JsonMapper;

/** Writes the JSON error body from servlet filters and security handlers (SB-07). */
public final class ErrorResponseWriter {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private ErrorResponseWriter() {}

  public static void write(HttpServletResponse response, int status, String error, String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.getOutputStream().write(JSON.writeValueAsBytes(ApiError.of(status, error, message)));
  }
}
