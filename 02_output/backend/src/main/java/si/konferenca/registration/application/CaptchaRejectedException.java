package si.konferenca.registration.application;

/** The reCAPTCHA token was not accepted (SR-01). */
public class CaptchaRejectedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public CaptchaRejectedException() {
    super("captcha rejected");
  }
}
