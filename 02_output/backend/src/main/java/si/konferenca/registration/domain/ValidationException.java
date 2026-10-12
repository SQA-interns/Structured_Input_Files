package si.konferenca.registration.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The registration breaks one or more field rules; nothing was stored. */
public class ValidationException extends RuntimeException {

  private static final long serialVersionUID = 1L;
  private final ArrayList<FieldError> errors;

  public ValidationException(List<FieldError> errors) {
    super("Registration is invalid");
    this.errors = new ArrayList<>(errors);
  }

  public List<FieldError> errors() {
    return Collections.unmodifiableList(errors);
  }
}
