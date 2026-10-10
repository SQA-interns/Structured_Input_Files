package si.konferenca.registration.domain;

/** A rejected field with its error code (contract openapi.yaml "FieldError"). */
public record FieldError(String field, String code) {

  public static final String REQUIRED = "REQUIRED";
  public static final String TOO_LONG = "TOO_LONG";
  public static final String INVALID_CHARACTERS = "INVALID_CHARACTERS";
  public static final String INVALID_EMAIL = "INVALID_EMAIL";
  public static final String NOT_ALLOWED = "NOT_ALLOWED";
  public static final String INVALID_VALUE = "INVALID_VALUE";
  public static final String UNKNOWN_OPTION = "UNKNOWN_OPTION";
  public static final String OPTION_NOT_AVAILABLE = "OPTION_NOT_AVAILABLE";
  public static final String DUPLICATE_OPTION = "DUPLICATE_OPTION";
  public static final String CONSENT_REQUIRED = "CONSENT_REQUIRED";
  public static final String UNKNOWN_CONSENT = "UNKNOWN_CONSENT";
}
