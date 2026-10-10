package si.konferenca.registration.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data access to {@code registration}; parameterised queries only (SB-05). */
interface RegistrationRepository extends JpaRepository<RegistrationEntity, Long> {

  boolean existsByEmailNormalized(String emailNormalized);

  List<RegistrationEntity> findAllByOrderBySubmittedAtAscIdAsc();
}
