package si.konferenca.registration.infrastructure.persistence;

import org.springframework.stereotype.Component;
import si.konferenca.registration.application.RegistrationStore;
import si.konferenca.registration.domain.Registration;

/** Stores registrations in PostgreSQL (database.sql). */
@Component
public class JpaRegistrationStore implements RegistrationStore {

  @Override
  public void save(Registration registration) {
    throw new UnsupportedOperationException("not implemented");
  }
}
