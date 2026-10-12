package si.konferenca.registration.service;

import java.util.List;

/** The submission is rejected; nothing was stored or sent. */
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
