package si.konferenca.registration.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import si.konferenca.registration.application.RegistrationReader;
import si.konferenca.registration.application.RegistrationStore;
import si.konferenca.registration.domain.Registration;

/** Stores and reads registrations in PostgreSQL (database.sql). */
@Component
public class JpaRegistrationStore implements RegistrationStore, RegistrationReader {

  @PersistenceContext private EntityManager entityManager;

  @Override
  @Transactional
  public void save(Registration registration) {
    entityManager.persist(RegistrationEntity.from(registration));
    entityManager.flush();
  }

  @Override
  public List<Registration> findAll() {
    throw new UnsupportedOperationException("not implemented");
  }
}
