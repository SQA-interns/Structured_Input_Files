package si.konferenca.registration.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import si.konferenca.registration.RegistrationProperties;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.service.RegistrationNotifier;

/**
 * Sends the participant confirmation and one organizer notification per address (spec 8). Each
 * failure is logged with the registration id only (OQ-03, SB-07).
 */
@Component
public class MailNotifier implements RegistrationNotifier {

  private static final Logger LOG = LoggerFactory.getLogger(MailNotifier.class);
  private static final String UTF_8 = StandardCharsets.UTF_8.name();

  private final JavaMailSender sender;
  private final RegistrationProperties properties;

  public MailNotifier(JavaMailSender sender, RegistrationProperties properties) {
    this.sender = sender;
    this.properties = properties;
  }

  @Override
  public void notifyParticipant(Registration registration) {
    String conference = properties.conferenceName();
    send(
        "participant confirmation",
        registration,
        registration.email(),
        EmailTexts.participantSubject(conference),
        EmailTexts.participantBody(registration, conference),
        null);
  }

  @Override
  public void notifyOrganizers(Registration registration, byte[] jsonCopy) {
    String conference = properties.conferenceName();
    for (String organizer : properties.mail().organizerRecipients()) {
      send(
          "organizer notification",
          registration,
          organizer,
          EmailTexts.organizerSubject(conference),
          EmailTexts.organizerBody(registration, conference),
          jsonCopy);
    }
  }

  private void send(
      String kind,
      Registration registration,
      String recipient,
      String subject,
      String body,
      byte[] attachment) {
    try {
      MimeMessage message = sender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, attachment != null, UTF_8);
      helper.setFrom(properties.mail().from());
      helper.setTo(singleAddress(recipient));
      helper.setSubject(subject);
      helper.setText(body, false);
      if (attachment != null) {
        helper.addAttachment(
            "registration-" + registration.id() + ".json",
            new ByteArrayResource(attachment),
            "application/json");
      }
      sender.send(message);
    } catch (MessagingException | MailException e) {
      LOG.warn("Email {} failed for registration {}", kind, registration.id());
    }
  }

  /** Exactly one address, without a display name; anything else is rejected. */
  private static InternetAddress singleAddress(String recipient) throws AddressException {
    InternetAddress address = new InternetAddress(recipient, true);
    address.validate();
    return new InternetAddress(address.getAddress());
  }
}
