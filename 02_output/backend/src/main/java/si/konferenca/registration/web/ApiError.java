package si.konferenca.registration.web;

import java.util.List;
import si.konferenca.registration.domain.FieldError;

/** openapi.json ErrorResponse. */
public record ApiError(String message, List<FieldError> fieldErrors) {

  static ApiError general(String message) {
    return new ApiError(message, List.of());
  }
}
