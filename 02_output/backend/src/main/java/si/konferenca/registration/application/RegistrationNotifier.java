package si.konferenca.registration.application;

import si.konferenca.registration.domain.Registration;

/** Port: emails about an accepted registration, sent only after it was stored (OQ-03). */
public interface RegistrationNotifier {

  void registrationAccepted(Registration registration);
}
