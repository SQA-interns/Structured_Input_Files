package si.konferenca.registration.service;

import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationRepository;
import si.konferenca.registration.infra.JsonCopyStore;

/**
 * Stores an accepted registration as database row and JSON copy in one unit (BR-07, AR-05): the
 * rows are flushed, the copy is written, then the transaction commits. A failed copy rolls the
 * transaction back; a failed commit removes the copy.
 */
@Component
public class RegistrationStore {

  private final RegistrationRepository repository;
  private final JsonCopyStore jsonCopies;

  public RegistrationStore(RegistrationRepository repository, JsonCopyStore jsonCopies) {
    this.repository = repository;
    this.jsonCopies = jsonCopies;
  }

  /** A stored registration and the bytes of its JSON copy. */
  public record StoredRegistration(Registration registration, byte[] jsonCopy) {}

  @Transactional
  public StoredRegistration save(Registration registration) {
    Registration saved = repository.saveAndFlush(registration);
    UUID id = saved.getId();
    byte[] copy = jsonCopies.write(saved);
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status != STATUS_COMMITTED) {
              jsonCopies.delete(id);
            }
          }
        });
    return new StoredRegistration(saved, copy);
  }
}
