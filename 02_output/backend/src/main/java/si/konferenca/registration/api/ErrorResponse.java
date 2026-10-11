package si.konferenca.registration.api;

import java.util.List;

/** The one error body shape of the API (openapi.json ErrorResponse). */
public record ErrorResponse(String message, List<FieldErrorDto> fieldErrors) {

  /** One offending field. */
  public record FieldErrorDto(String field, String message) {}

  public static ErrorResponse of(String message) {
    return new ErrorResponse(message, List.of());
  }
}
