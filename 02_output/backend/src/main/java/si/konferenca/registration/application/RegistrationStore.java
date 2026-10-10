package si.konferenca.registration.application;

import java.util.List;
import si.konferenca.registration.domain.AcceptedRegistration;

/** Port: the database of registrations (US-005). */
public interface RegistrationStore {

  /** Whether a registration with this normalised email exists (D-12). */
  boolean existsByNormalizedEmail(String normalizedEmail);

  /**
   * Inserts the registration and flushes, inside the caller's transaction.
   *
   * @throws DuplicateEmailException if the email is already registered
   */
  void insert(AcceptedRegistration registration);

  /** Every registration, oldest first (US-008). */
  List<AcceptedRegistration> findAllOrderedBySubmission();
}
