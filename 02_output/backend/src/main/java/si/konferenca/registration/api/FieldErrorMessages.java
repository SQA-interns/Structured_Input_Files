package si.konferenca.registration.api;

import java.util.Map;

/** Fixed English messages per field error code; no user input is echoed (SB-07). */
final class FieldErrorMessages {

  private static final Map<String, String> MESSAGES =
      Map.ofEntries(
          Map.entry("REQUIRED", "This field is required."),
          Map.entry("TOO_LONG", "This value is too long."),
          Map.entry("INVALID_CHARACTERS", "This value contains characters that are not allowed."),
          Map.entry("INVALID_EMAIL", "Enter a valid email address."),
          Map.entry("NOT_ALLOWED", "This field does not belong to the chosen registration type."),
          Map.entry("INVALID_VALUE", "This value is not valid."),
          Map.entry("UNKNOWN_OPTION", "An unknown activity was selected."),
          Map.entry("OPTION_NOT_AVAILABLE", "A selected activity is not available."),
          Map.entry("DUPLICATE_OPTION", "An activity was selected more than once."),
          Map.entry("CONSENT_REQUIRED", "This consent is required."),
          Map.entry("UNKNOWN_CONSENT", "An unknown consent was given."),
          Map.entry("CAPTCHA_FAILED", "The anti-robot check failed. Please try again."),
          Map.entry("DUPLICATE_EMAIL", "A registration with this email already exists."));

  private FieldErrorMessages() {}

  static String message(String code) {
    return MESSAGES.getOrDefault(code, "This value is not valid.");
  }
}
