package si.konferenca.registration.adapter.web;

import java.util.List;

/** Response bodies of `openapi.yaml`. */
public final class ApiResponses {

  private ApiResponses() {}

  /** `GeneralError`. */
  public record GeneralError(String message) {}

  /** `FieldError`. */
  public record FieldErrorBody(String field, String message) {}

  /** `ValidationError`. */
  public record ValidationError(String message, List<FieldErrorBody> errors) {

    public static ValidationError of(List<FieldErrorBody> errors) {
      return new ValidationError("Validation failed", errors);
    }
  }

  /** `FormOption`. */
  public record FormOption(String id, String name, String category, List<String> availableTo) {}

  /** `FormConsent`. */
  public record FormConsent(String id, String text, boolean mandatory) {}

  /** `CaptchaConfig`. */
  public record CaptchaConfig(String mode, String siteKey) {}

  /** `FormConfig`. */
  public record FormConfig(
      String conferenceName,
      List<FormOption> options,
      List<FormConsent> consents,
      CaptchaConfig captcha) {}
}
