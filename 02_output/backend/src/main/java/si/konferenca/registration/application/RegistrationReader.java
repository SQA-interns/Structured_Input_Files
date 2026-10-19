package si.konferenca.registration.application;

import java.util.List;
import si.konferenca.registration.domain.Registration;

/** Port: reads stored registrations (database.sql). */
public interface RegistrationReader {

  List<Registration> findAll();
}
