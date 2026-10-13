package si.konferenca.registration.domain;

/** The conference configuration violates its schema; the backend must not start (AC-003-03). */
public class InvalidConfigurationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public InvalidConfigurationException(String message) {
    super(message);
  }
}
