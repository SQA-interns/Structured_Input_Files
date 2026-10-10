package si.konferenca.registration.domain;

import java.util.List;

/** The form was rejected; carries every field error found (SB-01). */
public class ValidationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final transient List<FieldError> errors;

  public ValidationException(List<FieldError> errors) {
    super("registration form rejected");
    this.errors = List.copyOf(errors);
  }

  public List<FieldError> errors() {
    return errors;
  }
}
