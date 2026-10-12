package si.konferenca.registration.conference;

/** The options configuration cannot be used; the application must not start (AC-003-03). */
public class InvalidConferenceConfigurationException extends IllegalStateException {

  private static final long serialVersionUID = 1L;

  public InvalidConferenceConfigurationException(String message) {
    super("Invalid conference configuration: " + message);
  }

  public InvalidConferenceConfigurationException(String message, Throwable cause) {
    super("Invalid conference configuration: " + message, cause);
  }
}
