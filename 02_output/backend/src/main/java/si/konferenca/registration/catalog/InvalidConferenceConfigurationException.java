package si.konferenca.registration.catalog;

/** The conference configuration file is missing or violates its contract; startup stops. */
public class InvalidConferenceConfigurationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public InvalidConferenceConfigurationException(String message) {
    super(message);
  }

  public InvalidConferenceConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }
}
