package si.konferenca.registration.application;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import si.konferenca.registration.domain.AcceptedRegistration;

/**
 * Stores a registration in the database and as a JSON copy in one unit (AR-05, BR-07): the row is
 * inserted and flushed, then the copy written; a failed copy rolls the row back, a failed commit
 * deletes the copy.
 */
public class RegistrationRecorder {

  private final RegistrationStore store;
  private final JsonCopyStore copies;

  public RegistrationRecorder(RegistrationStore store, JsonCopyStore copies) {
    this.store = store;
    this.copies = copies;
  }

  /** Returns the bytes of the JSON copy once both are written. */
  @Transactional
  public byte[] record(AcceptedRegistration registration) {
    store.insert(registration);
    byte[] copy = copies.write(registration);
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status != STATUS_COMMITTED) {
              copies.delete(registration.reference());
            }
          }
        });
    return copy;
  }
}
