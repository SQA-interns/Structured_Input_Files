package si.konferenca.registration.domain;

import java.util.List;

/** Port: durable storage of accepted registrations (BR-07). */
public interface RegistrationStore {

  /**
   * Stores the registration in one transaction and runs {@code beforeCommit} inside it; if the
   * action fails, nothing is stored (AR-05).
   */
  void saveAtomically(Registration registration, Runnable beforeCommit);

  /** Every stored registration, oldest first. */
  List<Registration> findAll();
}
