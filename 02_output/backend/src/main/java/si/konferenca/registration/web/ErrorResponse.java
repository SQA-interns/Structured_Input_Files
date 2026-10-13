package si.konferenca.registration.web;

import java.util.Map;

/** OpenAPI schema ErrorResponse: a user text and field errors, no internals (SB-07). */
public record ErrorResponse(String message, Map<String, String> fieldErrors) {

  public ErrorResponse {
    fieldErrors = Map.copyOf(fieldErrors);
  }

  static ErrorResponse of(String message) {
    return new ErrorResponse(message, Map.of());
  }
}
