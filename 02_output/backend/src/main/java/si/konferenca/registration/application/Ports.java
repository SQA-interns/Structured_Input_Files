package si.konferenca.registration.application;

import java.util.List;
import java.util.UUID;
import si.konferenca.registration.domain.Registration;

/** The ports the use cases need; adapters implement them (spec §2). */
public interface Ports {

  /** Verifies an anti-automation token on the server side (SR-01). */
  interface CaptchaVerifier {
    boolean verify(String token, String clientIp);
  }

  /** Database storage of registrations (BR-07). */
  interface RegistrationStore {
    void save(Registration registration);

    /** Every stored registration, oldest first. */
    List<Registration> findAllOldestFirst();
  }

  /** Raw JSON copies on persistent storage (BR-07). */
  interface JsonCopyStore {
    /** Writes the copy and returns its exact bytes; fails with {@link StorageFailedException}. */
    byte[] write(Registration registration);

    void delete(UUID registrationId);
  }

  /** Participant and organizer emails (US-006, US-007). */
  interface RegistrationNotifier {
    void sendConfirmation(Registration registration);

    void notifyOrganizers(Registration registration, byte[] jsonCopy);
  }

  /** The Excel export (US-008). */
  interface WorkbookWriter {
    byte[] write(List<Registration> registrations);
  }
}
