package si.konferenca.registration.service;

import java.util.UUID;
import si.konferenca.registration.domain.Registration;

/** Port: the raw JSON copy of an accepted registration on persistent storage (BR-07). */
public interface JsonCopyStore {

  /** Writes {@code <id>.json} and returns its bytes; throws when it cannot be written. */
  byte[] write(Registration registration);

  /** Removes the copy if it exists. */
  void delete(UUID id);
}
