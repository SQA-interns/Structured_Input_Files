package si.konferenca.registration.adapter.web;

import java.util.List;

/** Response bodies of `openapi.yaml`. */
public interface ApiResponses {

  /** `GeneralError`. */
  record GeneralError(String message) {}

  /** `FieldError`. */
  record FieldErrorBody(String field, String message) {}

  /** `ValidationError`. */
  record ValidationError(String message, List<FieldErrorBody> errors) {

    static ValidationError of(List<FieldErrorBody> errors) {
      return new ValidationError("Validation failed", errors);
    }
  }

  /** `FormOption`. */
  record FormOption(String id, String name, String category, List<String> availableTo) {}

  /** `FormConsent`. */
  record FormConsent(String id, String text, boolean mandatory) {}

  /** `CaptchaConfig`. */
  record CaptchaConfig(String mode, String siteKey) {}

  /** `FormConfig`. */
  record FormConfig(
      String conferenceName,
      List<FormOption> options,
      List<FormConsent> consents,
      CaptchaConfig captcha) {}
}
