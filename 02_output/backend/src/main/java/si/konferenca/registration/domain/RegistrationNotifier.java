package si.konferenca.registration.domain;

/** Port: tells the participant and the organizers about an accepted registration. */
public interface RegistrationNotifier {

  /**
   * Called after the registration is stored; must never throw (D-07).
   *
   * @param jsonCopy the exact bytes of the stored JSON copy (US-007 attachment)
   */
  void registrationAccepted(Registration registration, byte[] jsonCopy);
}
