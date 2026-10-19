package si.konferenca.registration.domain;

/** The conference configuration violates conference-config.schema.json (AC-003-03). */
public class InvalidConfigurationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public InvalidConfigurationException(String problem) {
    super("Invalid conference configuration: " + problem);
  }

  public InvalidConfigurationException(String problem, Throwable cause) {
    super("Invalid conference configuration: " + problem, cause);
  }
}
