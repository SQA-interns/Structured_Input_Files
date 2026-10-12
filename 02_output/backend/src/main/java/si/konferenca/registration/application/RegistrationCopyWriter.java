package si.konferenca.registration.application;

import java.util.UUID;
import si.konferenca.registration.domain.Registration;

/** Port: the raw JSON copy on persistent storage (BR-07). */
public interface RegistrationCopyWriter {

  /** Writes the copy atomically and returns its bytes. */
  byte[] write(Registration registration);

  /** Removes the copy if it exists. */
  void delete(UUID registrationId);
}
