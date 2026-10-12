package si.konferenca.registration.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Stored registrations (parameterised queries only, SB-05). */
public interface RegistrationRepository extends JpaRepository<Registration, UUID> {

  List<Registration> findAllByOrderByRegisteredAtAsc();
}
