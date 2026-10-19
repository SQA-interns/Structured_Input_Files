package si.konferenca.registration.application;

import si.konferenca.registration.domain.Registration;

/** Port: the database (database.sql). */
public interface RegistrationStore {

  void save(Registration registration);
}
