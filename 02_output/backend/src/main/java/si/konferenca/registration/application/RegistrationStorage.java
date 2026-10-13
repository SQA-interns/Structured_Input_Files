package si.konferenca.registration.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import si.konferenca.registration.application.Ports.JsonCopyStore;
import si.konferenca.registration.application.Ports.RegistrationStore;
import si.konferenca.registration.domain.Registration;

/**
 * Stores the database row and the JSON copy in one transaction (AR-05, spec §4 step 5): the row is
 * flushed first, the copy is written before the commit, and a copy whose transaction does not
 * commit is deleted again.
 */
@Service
public class RegistrationStorage {

  private final RegistrationStore store;
  private final JsonCopyStore copies;

  public RegistrationStorage(RegistrationStore store, JsonCopyStore copies) {
    this.store = store;
    this.copies = copies;
  }

  /** Returns the exact bytes of the JSON copy. */
  @Transactional
  public byte[] store(Registration registration) {
    store.save(registration);
    byte[] copy = copies.write(registration);
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status != STATUS_COMMITTED) {
              copies.delete(registration.id());
            }
          }
        });
    return copy;
  }
}
