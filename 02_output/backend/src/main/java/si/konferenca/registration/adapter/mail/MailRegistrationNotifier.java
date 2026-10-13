package si.konferenca.registration.adapter.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import si.konferenca.registration.application.Ports.RegistrationNotifier;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.SubmissionValidator;

/**
 * Participant confirmation and organizer notification (`emails.md`, US-006, US-007). Messages are
 * plain text only and no header carries user input (SR-05); one recipient per message.
 */
@Component
public class MailRegistrationNotifier implements RegistrationNotifier {

  private static final Logger LOG = LoggerFactory.getLogger(MailRegistrationNotifier.class);

  private final JavaMailSender sender;
  private final String from;
  private final List<String> organizers;
  private final String conferenceName;

  public MailRegistrationNotifier(
      JavaMailSender sender,
      @Value("${app.mail.from}") String from,
      @Value("${app.mail.organizer-recipients}") String organizerRecipients,
      @Value("${app.conference.name}") String conferenceName) {
    this.sender = sender;
    this.from = from;
    this.organizers = parseRecipients(organizerRecipients);
    this.conferenceName = conferenceName;
  }

  /** ORGANIZER_EMAILS: comma separated, at least one, each a valid address. */
  static List<String> parseRecipients(String value) {
    List<String> addresses =
        Arrays.stream(value.split(",")).map(String::strip).filter(a -> !a.isEmpty()).toList();
    if (addresses.isEmpty()) {
      throw new IllegalStateException("ORGANIZER_EMAILS must contain at least one address");
    }
    for (String address : addresses) {
      if (!SubmissionValidator.isEmail(address)) {
        throw new IllegalStateException("ORGANIZER_EMAILS contains an invalid address");
      }
    }
    return addresses;
  }

  @Override
  public void sendConfirmation(Registration registration) {
    send(
        registration.email(),
        "Registration confirmation – " + conferenceName,
        MailTexts.confirmation(registration, conferenceName),
        null);
  }

  @Override
  public void notifyOrganizers(Registration registration, byte[] jsonCopy) {
    for (String organizer : organizers) {
      try {
        send(
            organizer,
            "New registration – " + conferenceName,
            MailTexts.organizerNotification(registration),
            new Attachment("registration-" + registration.id() + ".json", jsonCopy));
      } catch (MailException e) {
        LOG.warn(
            "Email organizer-notification for registration {} failed for one recipient ({})",
            registration.id(),
            e.getClass().getSimpleName());
      }
    }
  }

  private record Attachment(String fileName, byte[] content) {}

  private void send(String to, String subject, String text, Attachment attachment) {
    MimeMessage message = sender.createMimeMessage();
    try {
      MimeMessageHelper helper = new MimeMessageHelper(message, attachment != null, "UTF-8");
      helper.setFrom(from);
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(text, false);
      if (attachment != null) {
        helper.addAttachment(
            attachment.fileName(), new ByteArrayResource(attachment.content()), "application/json");
      }
    } catch (MessagingException e) {
      throw new MailPreparationException("Message could not be prepared", e);
    }
    sender.send(message);
  }
}
