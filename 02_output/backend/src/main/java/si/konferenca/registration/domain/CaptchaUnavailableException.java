package si.konferenca.registration.domain;

/** The captcha verifier could not be reached or answered unexpectedly. */
public class CaptchaUnavailableException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public CaptchaUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
