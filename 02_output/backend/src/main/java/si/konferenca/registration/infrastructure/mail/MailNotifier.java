package si.konferenca.registration.infrastructure.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import si.konferenca.registration.application.Notifier;
import si.konferenca.registration.domain.Category;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.SelectedOption;

/**
 * Plain-text emails (contract {@code emails.md}): user input only in the UTF-8 body, never in a
 * header except the validated participant address (SR-05); only registration data (SR-07).
 */
public class MailNotifier implements Notifier {

  private static final Logger LOG = LoggerFactory.getLogger(MailNotifier.class);
  private static final String UTF_8 = StandardCharsets.UTF_8.name();

  private final JavaMailSender sender;
  private final String from;
  private final String conferenceName;
  private final List<String> organizers;

  public MailNotifier(
      JavaMailSender sender, String from, String conferenceName, List<String> organizers) {
    this.sender = sender;
    this.from = from;
    this.conferenceName = conferenceName;
    this.organizers = List.copyOf(organizers);
  }

  @Override
  public void registrationAccepted(Registration registration, byte[] jsonCopy) {
    send("PARTICIPANT", registration, () -> participantMessage(registration));
    for (String organizer : organizers) {
      send("ORGANIZER", registration, () -> organizerMessage(registration, organizer, jsonCopy));
    }
  }

  private interface MessageFactory {
    MimeMessage create() throws MessagingException;
  }

  private void send(String kind, Registration registration, MessageFactory factory) {
    try {
      sender.send(factory.create());
    } catch (MessagingException | MailException e) {
      LOG.warn("email failed kind={} registration={}", kind, registration.id());
    }
  }

  private MimeMessage participantMessage(Registration r) throws MessagingException {
    MimeMessage message = sender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, false, UTF_8);
    helper.setFrom(from);
    helper.setTo(new InternetAddress(r.email(), true));
    helper.setSubject("Registration confirmation – " + conferenceName);
    helper.setText(participantText(r), false);
    return message;
  }

  private MimeMessage organizerMessage(Registration r, String organizer, byte[] jsonCopy)
      throws MessagingException {
    MimeMessage message = sender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true, UTF_8);
    helper.setFrom(from);
    helper.setTo(new InternetAddress(organizer, true));
    helper.setSubject("New registration – " + r.type().label() + " – " + conferenceName);
    helper.setText(organizerText(r), false);
    helper.addAttachment(
        "registration-" + r.id() + ".json", new ByteArrayResource(jsonCopy), "application/json");
    return message;
  }

  String participantText(Registration r) {
    StringBuilder text = new StringBuilder(256);
    text.append("Dear ")
        .append(r.firstName())
        .append(' ')
        .append(r.lastName())
        .append(",\n\nthank you for registering for ")
        .append(conferenceName)
        .append(".\n\nRegistration type: ")
        .append(r.type().label())
        .append("\nRegistration ID: ")
        .append(r.id())
        .append("\n\n");
    appendOptions(text, r.selectedOptions());
    return text.toString();
  }

  String organizerText(Registration r) {
    StringBuilder text = new StringBuilder(512);
    text.append("A new registration was received for ").append(conferenceName).append(".\n\n");
    line(text, "Registration ID", r.id().toString());
    line(text, "Type", r.type().label());
    line(text, "Submitted at", DateTimeFormatter.ISO_INSTANT.format(r.submittedAt()));
    line(text, "First name", r.firstName());
    line(text, "Last name", r.lastName());
    line(text, "Email", r.email());
    line(text, "Organization / institution", r.organization());
    line(text, "Study institution", r.studyInstitution());
    line(text, "Study programme", r.studyProgramme());
    line(text, "Student ID", r.studentId());
    text.append('\n');
    appendOptions(text, r.selectedOptions());
    text.append('\n');
    line(text, "Consent", r.consent().text());
    line(text, "Consent given at", DateTimeFormatter.ISO_INSTANT.format(r.consent().givenAt()));
    text.append("\nThe registration as stored is attached as JSON.\n");
    return text.toString();
  }

  private static void line(StringBuilder text, String label, String value) {
    if (value != null) {
      text.append(label).append(": ").append(value).append('\n');
    }
  }

  private static void appendOptions(StringBuilder text, List<SelectedOption> options) {
    if (options.isEmpty()) {
      text.append("No optional activities selected.\n");
      return;
    }
    text.append("Selected activities:\n");
    for (Category category : Category.values()) {
      List<String> names =
          options.stream().filter(o -> o.category() == category).map(SelectedOption::name).toList();
      if (!names.isEmpty()) {
        text.append(category.label()).append(": ").append(String.join("; ", names)).append('\n');
      }
    }
  }
}
