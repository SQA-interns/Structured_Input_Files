package si.konferenca.registration.application;

import si.konferenca.registration.domain.Registration;

/** Port: the raw JSON copy on persistent storage (registration-copy.schema.json, BR-07). */
public interface JsonCopyStore {

  /** Writes the copy; throws when it cannot be written, so the registration is not accepted. */
  void write(Registration registration);
}
