package si.konferenca.registration.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationRepository;

/** Stores an accepted registration in one database transaction (BR-07, AR-05). */
@Component
public class RegistrationStore {

  private final RegistrationRepository repository;

  public RegistrationStore(RegistrationRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public Registration save(Registration registration) {
    return repository.saveAndFlush(registration);
  }
}
