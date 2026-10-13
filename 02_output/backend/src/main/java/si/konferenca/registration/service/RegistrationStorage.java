package si.konferenca.registration.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.persistence.RegistrationStore;

/** Database row and JSON copy in one unit: both are written or neither remains (AR-05). */
@Component
public class RegistrationStorage {

  private final RegistrationStore store;
  private final JsonCopyStore jsonCopies;

  public RegistrationStorage(RegistrationStore store, JsonCopyStore jsonCopies) {
    this.store = store;
    this.jsonCopies = jsonCopies;
  }

  /** Returns the bytes of the JSON copy; any failure rolls the row back and removes the copy. */
  @Transactional
  public byte[] store(Registration registration) {
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status != STATUS_COMMITTED) {
              jsonCopies.delete(registration.id());
            }
          }
        });
    store.insert(registration);
    return jsonCopies.write(registration);
  }
}
