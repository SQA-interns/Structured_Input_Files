package si.konferenca.registration.adapter.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data access to `registration` (bound parameters only, SB-05). */
public interface RegistrationRepository extends JpaRepository<RegistrationEntity, UUID> {}
