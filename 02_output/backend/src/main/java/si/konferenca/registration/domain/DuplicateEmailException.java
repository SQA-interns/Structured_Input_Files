package si.konferenca.registration.domain;

/** A registration with this email already exists (D-14). */
public class DuplicateEmailException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public DuplicateEmailException() {
    super("Email already registered");
  }
}
