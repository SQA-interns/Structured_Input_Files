package si.konferenca.registration.application;

/** A registration could not be stored completely; nothing of it remains (AC-005-03). */
public class StorageFailedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public StorageFailedException(String message, Throwable cause) {
    super(message, cause);
  }
}
