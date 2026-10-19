package si.konferenca.registration.infrastructure.mail;

import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import si.konferenca.registration.application.RegistrationNotifier;
import si.konferenca.registration.domain.Registration;

/** Sends the emails on an executor; a failure is logged with the registration id only. */
public class MailRegistrationNotifier implements RegistrationNotifier {

  private static final Logger LOG = LoggerFactory.getLogger(MailRegistrationNotifier.class);

  private final JavaMailSender sender;
  private final ParticipantMailComposer participantMail;
  private final Executor executor;

  public MailRegistrationNotifier(
      JavaMailSender sender, ParticipantMailComposer participantMail, Executor executor) {
    this.sender = sender;
    this.participantMail = participantMail;
    this.executor = executor;
  }

  @Override
  public void registrationAccepted(Registration registration) {
    executor.execute(() -> sendConfirmation(registration));
  }

  private void sendConfirmation(Registration registration) {
    try {
      sender.send(participantMail.compose(registration));
    } catch (RuntimeException e) {
      LOG.warn("Confirmation email for registration {} could not be sent", registration.id());
    }
  }
}
