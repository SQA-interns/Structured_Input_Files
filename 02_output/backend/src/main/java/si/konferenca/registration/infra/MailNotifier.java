package si.konferenca.registration.infra;

import jakarta.mail.internet.InternetAddress;
import java.nio.charset.StandardCharsets;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import si.konferenca.registration.config.AppProperties;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;
import si.konferenca.registration.domain.SelectedOption;

/**
 * Emails after a registration is stored (docs/02_contracts/emails.json). Plain text only; user
 * input never appears in a header other than the validated participant address (SR-05).
 */
@Component
public class MailNotifier {

  private final JavaMailSender mailSender;
  private final String from;
  private final String conferenceName;

  public MailNotifier(JavaMailSender mailSender, AppProperties properties) {
    this.mailSender = mailSender;
    this.from = properties.mail().from();
    this.conferenceName = properties.conference().name();
  }

  /** The confirmation to the participant (US-006). */
  public void sendParticipantConfirmation(Registration registration) {
    mailSender.send(
        message -> {
          MimeMessageHelper helper =
              new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
          helper.setFrom(new InternetAddress(from, true));
          helper.setTo(new InternetAddress(registration.getEmail(), true));
          helper.setSubject("Registration confirmed: " + conferenceName);
          helper.setText(participantText(registration), false);
        });
  }

  String participantText(Registration registration) {
    StringBuilder text = new StringBuilder();
    text.append("Dear ")
        .append(registration.getFirstName())
        .append(' ')
        .append(registration.getLastName())
        .append(",\n\nthank you for registering for ")
        .append(conferenceName)
        .append(".\n\nRegistration type: ")
        .append(
            registration.getType() == RegistrationType.STUDENT ? "Student" : "External participant")
        .append("\n\n");
    appendOptions(text, registration);
    text.append("\nRegistration ID: ")
        .append(registration.getId())
        .append("\n\n")
        .append(conferenceName)
        .append('\n');
    return text.toString();
  }

  static void appendOptions(StringBuilder text, Registration registration) {
    if (registration.getOptions().isEmpty()) {
      text.append("Selected options: none\n");
      return;
    }
    text.append("Selected options:\n");
    for (SelectedOption option : registration.getOptions()) {
      text.append("- ").append(option.optionName()).append('\n');
    }
  }
}
