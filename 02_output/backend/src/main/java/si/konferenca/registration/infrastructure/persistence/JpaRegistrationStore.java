package si.konferenca.registration.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import java.sql.SQLException;
import java.util.List;
import org.springframework.stereotype.Component;
import si.konferenca.registration.application.RegistrationStore;
import si.konferenca.registration.domain.DuplicateEmailException;
import si.konferenca.registration.domain.Registration;

/** {@link RegistrationStore} on PostgreSQL through JPA, with bound parameters only (SB-05). */
@Component
class JpaRegistrationStore implements RegistrationStore {

  private static final String UNIQUE_VIOLATION = "23505";

  @PersistenceContext private EntityManager entityManager;

  @Override
  public boolean emailExists(String normalizedEmail) {
    Long count =
        entityManager
            .createQuery(
                "select count(r) from RegistrationEntity r where r.emailNormalized = :email",
                Long.class)
            .setParameter("email", normalizedEmail)
            .getSingleResult();
    return count > 0;
  }

  @Override
  public void save(Registration registration) {
    try {
      entityManager.persist(RegistrationEntity.from(registration));
      entityManager.flush();
    } catch (PersistenceException e) {
      if (isUniqueViolation(e)) {
        throw new DuplicateEmailException();
      }
      throw e;
    }
  }

  @Override
  public List<Registration> findAllBySubmissionTime() {
    return entityManager
        .createQuery(
            "select r from RegistrationEntity r order by r.submittedAt, r.id",
            RegistrationEntity.class)
        .getResultList()
        .stream()
        .map(RegistrationEntity::toDomain)
        .toList();
  }

  private static boolean isUniqueViolation(Throwable e) {
    for (Throwable t = e; t != null; t = t.getCause()) {
      if (t instanceof SQLException sql && UNIQUE_VIOLATION.equals(sql.getSQLState())) {
        return true;
      }
    }
    return false;
  }
}
