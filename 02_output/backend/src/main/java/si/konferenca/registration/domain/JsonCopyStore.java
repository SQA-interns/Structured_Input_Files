package si.konferenca.registration.domain;

import java.util.UUID;

/** Port: the raw JSON copy of an accepted registration on persistent storage (BR-07). */
public interface JsonCopyStore {

  /** Writes the copy and returns the exact bytes written. */
  byte[] write(Registration registration);

  /** Removes the copy if it exists. */
  void delete(UUID id);
}
