package si.konferenca.registration.application;

import java.util.List;
import si.konferenca.registration.domain.Registration;

/** Port: the registration database (BR-07). */
public interface RegistrationStore {

  boolean emailExists(String normalizedEmail);

  /**
   * Inserts and flushes the registration inside the current transaction.
   *
   * @throws si.konferenca.registration.domain.DuplicateEmailException if the email is taken
   */
  void save(Registration registration);

  List<Registration> findAllBySubmissionTime();
}
