package si.konferenca.registration.application;

/** A registration with the same email already exists (D-12). */
public class DuplicateEmailException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public DuplicateEmailException() {
    super("duplicate email");
  }
}
