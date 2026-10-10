package si.konferenca.registration.application;

import java.util.UUID;
import si.konferenca.registration.domain.AcceptedRegistration;

/** Port: raw JSON copies on persistent storage (BR-07, contract registration-copy.schema.json). */
public interface JsonCopyStore {

  /**
   * Writes the copy durably and returns its exact bytes.
   *
   * @throws java.io.UncheckedIOException if the copy cannot be written
   */
  byte[] write(AcceptedRegistration registration);

  /** Removes the copy of a registration whose transaction did not commit. */
  void delete(UUID reference);
}
