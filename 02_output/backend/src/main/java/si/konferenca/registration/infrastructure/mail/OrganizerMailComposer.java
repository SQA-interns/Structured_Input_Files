package si.konferenca.registration.infrastructure.mail;

import jakarta.mail.internet.MimeMessage;
import java.util.List;
import org.springframework.mail.javamail.JavaMailSender;
import si.konferenca.registration.domain.Registration;

/** The organizer notification (email.md): one message per address with the JSON copy. */
public class OrganizerMailComposer {

  public OrganizerMailComposer(
      JavaMailSender sender, String from, String conferenceName, List<String> organizers) {}

  public List<MimeMessage> compose(Registration registration) {
    throw new UnsupportedOperationException("not implemented");
  }
}
