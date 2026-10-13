package si.konferenca.registration.service;

import si.konferenca.registration.domain.Registration;

/** Port: emails about an accepted registration (US-006, US-007). */
public interface RegistrationNotifier {

  void notifyParticipant(Registration registration);

  void notifyOrganizers(Registration registration, byte[] jsonCopy);
}
