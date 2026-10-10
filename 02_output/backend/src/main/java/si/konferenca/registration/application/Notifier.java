package si.konferenca.registration.application;

import si.konferenca.registration.domain.AcceptedRegistration;

/** Port: emails sent after a registration is stored (US-006, US-007). */
public interface Notifier {

  /** Plain-text confirmation to the participant (US-006). */
  void sendParticipantConfirmation(AcceptedRegistration registration);
}
