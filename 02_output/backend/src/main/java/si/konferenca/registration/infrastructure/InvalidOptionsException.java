package si.konferenca.registration.infrastructure;

/** The options configuration file is invalid; the backend must not start (AC-003-03). */
public class InvalidOptionsException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public InvalidOptionsException(String message) {
    super("Invalid conference options configuration: " + message);
  }
}
