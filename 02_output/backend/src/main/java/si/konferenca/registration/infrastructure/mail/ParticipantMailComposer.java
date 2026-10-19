package si.konferenca.registration.infrastructure.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.Registration;

/** The participant confirmation (email.md): plain text, fixed headers (SR-05). */
public class ParticipantMailComposer {

  private final JavaMailSender sender;
  private final String from;
  private final String conferenceName;

  public ParticipantMailComposer(JavaMailSender sender, String from, String conferenceName) {
    this.sender = sender;
    this.from = from;
    this.conferenceName = conferenceName;
  }

  public MimeMessage compose(Registration registration) {
    StringBuilder text = new StringBuilder();
    text.append("Dear ")
        .append(registration.firstName())
        .append(' ')
        .append(registration.lastName())
        .append(",\n\nthank you for registering for ")
        .append(conferenceName)
        .append(".\n\nSelected options:\n");
    if (registration.options().isEmpty()) {
      text.append("none\n");
    }
    for (ConferenceOption option : registration.options()) {
      text.append("- ").append(option.name()).append('\n');
    }
    text.append("\nRegistration ID: ").append(registration.id()).append("\n\nKind regards,\n");
    text.append(conferenceName).append('\n');
    try {
      MimeMessage message = sender.createMimeMessage();
      MimeMessageHelper helper =
          new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
      helper.setFrom(from);
      helper.setTo(new InternetAddress(registration.email(), true));
      helper.setSubject("Registration confirmation - " + conferenceName);
      helper.setText(text.toString(), false);
      message.saveChanges();
      return message;
    } catch (MessagingException e) {
      throw new MailPreparationException("confirmation for " + registration.id(), e);
    }
  }
}
