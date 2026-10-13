package si.konferenca.registration.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data access to {@link RegistrationEntity}. */
public interface RegistrationRepository extends JpaRepository<RegistrationEntity, UUID> {

  List<RegistrationEntity> findAllByOrderByRegisteredAtAsc();
}
