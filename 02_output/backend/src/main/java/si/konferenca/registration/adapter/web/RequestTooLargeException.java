package si.konferenca.registration.adapter.web;

import java.io.IOException;

/** The request body exceeds the configured limit (SR-03). */
public class RequestTooLargeException extends IOException {

  private static final long serialVersionUID = 1L;

  public RequestTooLargeException() {
    super("Request body too large");
  }

  /** Whether the exception or one of its causes is a {@link RequestTooLargeException}. */
  static boolean isCause(Throwable error) {
    for (Throwable t = error; t != null; t = t.getCause()) {
      if (t instanceof RequestTooLargeException) {
        return true;
      }
    }
    return false;
  }
}
