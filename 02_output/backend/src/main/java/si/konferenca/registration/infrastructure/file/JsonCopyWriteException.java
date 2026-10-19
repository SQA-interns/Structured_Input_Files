package si.konferenca.registration.infrastructure.file;

/** The JSON copy could not be written (AC-005-03). */
public class JsonCopyWriteException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public JsonCopyWriteException(String message, Throwable cause) {
    super(message, cause);
  }
}
