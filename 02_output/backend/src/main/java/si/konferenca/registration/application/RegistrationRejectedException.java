package si.konferenca.registration.application;

import java.util.List;
import si.konferenca.registration.domain.FieldError;

/** The submission is rejected with field errors; nothing was stored (HTTP 400). */
public class RegistrationRejectedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final transient List<FieldError> fieldErrors;

  public RegistrationRejectedException(List<FieldError> fieldErrors) {
    super("Registration rejected");
    this.fieldErrors = List.copyOf(fieldErrors);
  }

  public List<FieldError> fieldErrors() {
    return fieldErrors;
  }
}
