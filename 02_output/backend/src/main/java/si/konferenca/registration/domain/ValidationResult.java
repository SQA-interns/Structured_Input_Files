package si.konferenca.registration.domain;

import java.util.List;

/** Either field errors or the validated registration. */
public record ValidationResult(List<FieldError> errors, ValidatedRegistration registration) {

  public boolean isValid() {
    return errors.isEmpty();
  }
}
