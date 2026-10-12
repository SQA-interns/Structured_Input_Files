package si.konferenca.registration.infra;

import jakarta.activation.DataHandler;
import jakarta.mail.MessagingException;
import jakarta.mail.Part;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.util.ByteArrayDataSource;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import si.konferenca.registration.config.AppProperties;
import si.konferenca.registration.domain.GivenConsent;
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
  private final List<String> organizerEmails;

  public MailNotifier(JavaMailSender mailSender, AppProperties properties) {
    this.mailSender = mailSender;
    this.from = properties.mail().from();
    this.conferenceName = properties.conference().name();
    this.organizerEmails = properties.organizer().emails().stream().map(String::trim).toList();
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

  /**
   * One notification per organizer address with the JSON copy attached (US-007); returns the number
   * of addresses that could not be notified.
   */
  public int sendOrganizerNotifications(Registration registration, byte[] jsonCopy) {
    int failed = 0;
    String text = organizerText(registration);
    for (String organizer : organizerEmails) {
      try {
        mailSender.send(
            message -> {
              MimeMessageHelper helper =
                  new MimeMessageHelper(
                      message,
                      MimeMessageHelper.MULTIPART_MODE_MIXED,
                      StandardCharsets.UTF_8.name());
              helper.setFrom(new InternetAddress(from, true));
              helper.setTo(new InternetAddress(organizer, true));
              helper.setSubject("New registration " + registration.getId());
              helper.setText(text, false);
              helper.getMimeMultipart().addBodyPart(jsonAttachment(registration, jsonCopy));
            });
      } catch (MailException e) {
        failed++;
      }
    }
    return failed;
  }

  /** Base64 keeps the attachment byte-identical to the stored copy (AC-007-02). */
  static MimeBodyPart jsonAttachment(Registration registration, byte[] jsonCopy)
      throws MessagingException {
    MimeBodyPart part = new MimeBodyPart();
    part.setDataHandler(new DataHandler(new ByteArrayDataSource(jsonCopy, "application/json")));
    part.setFileName("registration-" + registration.getId() + ".json");
    part.setDisposition(Part.ATTACHMENT);
    part.setHeader("Content-Transfer-Encoding", "base64");
    return part;
  }

  String organizerText(Registration registration) {
    StringBuilder text = new StringBuilder();
    text.append("New registration for ")
        .append(conferenceName)
        .append("\n\nRegistration ID: ")
        .append(registration.getId())
        .append("\nType: ")
        .append(registration.getType().name())
        .append("\nRegistered at: ")
        .append(registration.getRegisteredAt())
        .append("\nFirst name: ")
        .append(registration.getFirstName())
        .append("\nLast name: ")
        .append(registration.getLastName())
        .append("\nEmail: ")
        .append(registration.getEmail())
        .append('\n');
    appendIfPresent(text, "Organization", registration.getOrganization());
    appendIfPresent(text, "Study institution", registration.getStudyInstitution());
    appendIfPresent(text, "Study programme", registration.getStudyProgramme());
    appendIfPresent(text, "Student ID", registration.getStudentId());
    text.append('\n');
    appendOptions(text, registration);
    text.append("\nConsents:\n");
    for (GivenConsent consent : registration.getConsents()) {
      text.append("- ")
          .append(consent.consentId())
          .append(" (given at ")
          .append(consent.givenAt())
          .append(")\n");
    }
    text.append("\nThe registration as accepted is attached as JSON.\n");
    return text.toString();
  }

  private static void appendIfPresent(StringBuilder text, String label, String value) {
    if (value != null) {
      text.append(label).append(": ").append(value).append('\n');
    }
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
