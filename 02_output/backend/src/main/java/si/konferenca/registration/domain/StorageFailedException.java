package si.konferenca.registration.domain;

/** The registration could not be stored completely; nothing was kept (AR-05). */
public class StorageFailedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public StorageFailedException(Throwable cause) {
    super("Registration could not be stored", cause);
  }
}
