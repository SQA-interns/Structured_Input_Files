package si.konferenca.registration.infrastructure.mail;

import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import si.konferenca.registration.domain.Registration;

/** The participant confirmation (email.md): plain text, fixed headers (SR-05). */
public class ParticipantMailComposer {

  public ParticipantMailComposer(JavaMailSender sender, String from, String conferenceName) {}

  public MimeMessage compose(Registration registration) {
    throw new UnsupportedOperationException("not implemented");
  }
}
