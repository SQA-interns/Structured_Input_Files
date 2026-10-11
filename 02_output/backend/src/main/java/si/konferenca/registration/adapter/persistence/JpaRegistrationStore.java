package si.konferenca.registration.adapter.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationStore;

/** PostgreSQL storage through JPA; parameters are always bound (SB-05). */
@Component
public class JpaRegistrationStore implements RegistrationStore {

  @PersistenceContext private EntityManager em;

  @Override
  @Transactional
  public void saveAtomically(Registration registration, Runnable beforeCommit) {
    em.persist(new RegistrationEntity(registration));
    em.flush();
    beforeCommit.run();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Registration> findAll() {
    return em
        .createQuery(
            "select distinct r from RegistrationEntity r"
                + " left join fetch r.options left join fetch r.consents"
                + " order by r.registeredAt",
            RegistrationEntity.class)
        .getResultList()
        .stream()
        .map(RegistrationEntity::toDomain)
        .toList();
  }
}
