package si.konferenca.registration.service;

import java.util.Map;
import si.konferenca.registration.domain.FieldErrors;

/** The submission was rejected with field errors; nothing was stored or sent. */
public class RegistrationRejectedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final Map<String, String> fieldErrors;

  public RegistrationRejectedException(FieldErrors errors) {
    super("Registration rejected");
    this.fieldErrors = Map.copyOf(errors.asMap());
  }

  public Map<String, String> fieldErrors() {
    return fieldErrors;
  }
}
