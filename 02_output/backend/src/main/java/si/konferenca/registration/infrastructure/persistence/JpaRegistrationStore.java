package si.konferenca.registration.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import si.konferenca.registration.application.RegistrationStore;
import si.konferenca.registration.domain.Registration;

/** Stores registrations in PostgreSQL (database.sql). */
@Component
public class JpaRegistrationStore implements RegistrationStore {

  @PersistenceContext private EntityManager entityManager;

  @Override
  @Transactional
  public void save(Registration registration) {
    entityManager.persist(RegistrationEntity.from(registration));
    entityManager.flush();
  }
}
