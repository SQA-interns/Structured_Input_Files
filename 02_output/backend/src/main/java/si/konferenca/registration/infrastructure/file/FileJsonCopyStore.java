package si.konferenca.registration.infrastructure.file;

import java.nio.file.Path;
import si.konferenca.registration.application.JsonCopyStore;
import si.konferenca.registration.domain.Registration;

/** Writes {@code <dir>/<id>.json} (registration-copy.schema.json). */
public class FileJsonCopyStore implements JsonCopyStore {

  public FileJsonCopyStore(Path directory) {}

  /** The exact bytes of the copy of a registration. */
  public static byte[] content(Registration registration) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void write(Registration registration) {
    throw new UnsupportedOperationException("not implemented");
  }
}
