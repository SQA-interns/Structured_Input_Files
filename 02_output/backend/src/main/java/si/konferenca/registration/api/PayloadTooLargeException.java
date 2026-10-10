package si.konferenca.registration.api;

import java.io.IOException;

/** A request body exceeded the configured limit while being read (SR-03). */
public class PayloadTooLargeException extends IOException {

  private static final long serialVersionUID = 1L;

  public PayloadTooLargeException() {
    super("request body too large");
  }
}
