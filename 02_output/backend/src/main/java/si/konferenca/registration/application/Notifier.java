package si.konferenca.registration.application;

import si.konferenca.registration.domain.Registration;

/** Port: emails after a registration is stored (US-006, US-007). */
public interface Notifier {

  /**
   * Sends the participant confirmation and the organizer notifications. Never throws: a failed
   * email is logged and does not affect the accepted registration (D-12).
   */
  void registrationAccepted(Registration registration, byte[] jsonCopy);
}
