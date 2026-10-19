package si.konferenca.registration.infrastructure.mail;

import java.util.concurrent.Executor;
import org.springframework.mail.javamail.JavaMailSender;
import si.konferenca.registration.application.RegistrationNotifier;
import si.konferenca.registration.domain.Registration;

/** Sends the emails on an executor; a failure is logged with the registration id only. */
public class MailRegistrationNotifier implements RegistrationNotifier {

  public MailRegistrationNotifier(
      JavaMailSender sender, ParticipantMailComposer participantMail, Executor executor) {}

  @Override
  public void registrationAccepted(Registration registration) {
    throw new UnsupportedOperationException("not implemented");
  }
}
