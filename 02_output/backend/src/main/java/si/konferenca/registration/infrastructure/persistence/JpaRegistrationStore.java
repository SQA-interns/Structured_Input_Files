package si.konferenca.registration.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import si.konferenca.registration.application.RegistrationReader;
import si.konferenca.registration.application.RegistrationStore;
import si.konferenca.registration.domain.ConferenceConfiguration;
import si.konferenca.registration.domain.Registration;

/** Stores and reads registrations in PostgreSQL (database.sql). */
@Component
public class JpaRegistrationStore implements RegistrationStore, RegistrationReader {

  @PersistenceContext private EntityManager entityManager;

  private final ConferenceConfiguration configuration;

  public JpaRegistrationStore(ConferenceConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  @Transactional
  public void save(Registration registration) {
    entityManager.persist(RegistrationEntity.from(registration));
    entityManager.flush();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Registration> findAll() {
    return entityManager
        .createQuery(
            "select r from RegistrationEntity r order by r.registeredAt", RegistrationEntity.class)
        .getResultList()
        .stream()
        .map(entity -> entity.toDomain(configuration))
        .toList();
  }
}
