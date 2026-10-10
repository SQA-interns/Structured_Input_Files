package si.konferenca.registration.api;

import java.util.List;

/** Error body of every failed request (contract openapi.yaml "Error", SB-07). */
public record ApiError(int status, String error, String message, List<FieldErrorBody> fieldErrors) {

  public static ApiError of(int status, String error, String message) {
    return new ApiError(status, error, message, List.of());
  }

  /** One field error. */
  public record FieldErrorBody(String field, String code, String message) {}
}
