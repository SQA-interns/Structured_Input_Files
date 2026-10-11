package si.konferenca.registration.domain;

import java.util.List;

/** The submission breaks a business rule; nothing was stored. */
public class RegistrationRejectedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final transient List<FieldError> errors;

  public RegistrationRejectedException(List<FieldError> errors) {
    super("Registration rejected");
    this.errors = List.copyOf(errors);
  }

  public List<FieldError> errors() {
    return errors;
  }
}
