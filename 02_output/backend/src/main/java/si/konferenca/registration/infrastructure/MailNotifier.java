package si.konferenca.registration.infrastructure;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import si.konferenca.registration.application.Notifier;
import si.konferenca.registration.domain.AcceptedRegistration;
import si.konferenca.registration.domain.AcceptedRegistration.SelectedOption;

/**
 * SMTP adapter (contract emails.yaml): UTF-8 plain text only, so participant input stays literal;
 * headers carry only configuration values and the reference (SR-05).
 */
public class MailNotifier implements Notifier {

  private final JavaMailSender sender;
  private final MailSettings settings;

  public MailNotifier(JavaMailSender sender, MailSettings settings) {
    this.sender = sender;
    this.settings = settings;
  }

  @Override
  public void sendParticipantConfirmation(AcceptedRegistration registration) {
    String reference = registration.reference().toString();
    send(
        List.of(registration.participant().email()),
        settings.conferenceName() + " – registration confirmed (" + reference + ")",
        MailTexts.participantConfirmation(registration, settings.conferenceName()));
  }

  @Override
  public void sendOrganizerNotification(AcceptedRegistration registration, byte[] jsonCopy) {
    String reference = registration.reference().toString();
    try {
      MimeMessage message = sender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      fill(
          helper,
          settings.organizerEmails(),
          settings.conferenceName() + " – new registration " + reference,
          MailTexts.organizerNotification(registration));
      helper.addAttachment(
          "registration-" + reference + ".json",
          new ByteArrayResource(jsonCopy),
          "application/json");
      sender.send(message);
    } catch (MessagingException e) {
      throw new IllegalStateException("email could not be composed", e);
    }
  }

  private void send(List<String> to, String subject, String text) {
    try {
      MimeMessage message = sender.createMimeMessage();
      fill(new MimeMessageHelper(message, false, "UTF-8"), to, subject, text);
      sender.send(message);
    } catch (MessagingException e) {
      throw new IllegalStateException("email could not be composed", e);
    }
  }

  private void fill(MimeMessageHelper helper, List<String> to, String subject, String text)
      throws MessagingException {
    helper.setFrom(settings.sender());
    helper.setTo(to.toArray(String[]::new));
    helper.setSubject(subject);
    helper.setText(text, false);
  }

  /** Mail configuration; values come from configuration, never from participants. */
  public record MailSettings(String sender, String conferenceName, List<String> organizerEmails) {

    public MailSettings {
      conferenceName = conferenceName.replaceAll("\\p{Cntrl}", " ");
      organizerEmails =
          organizerEmails.stream().map(String::strip).filter(s -> !s.isEmpty()).toList();
    }
  }

  static String optionLine(SelectedOption option) {
    return "- " + option.name() + " (" + option.category().label() + ")";
  }
}
