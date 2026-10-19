package si.konferenca.registration.infrastructure.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.Timestamps;
import si.konferenca.registration.infrastructure.file.FileJsonCopyStore;

/** The organizer notification (email.md): one message per address with the JSON copy. */
public class OrganizerMailComposer {

  private final JavaMailSender sender;
  private final String from;
  private final String conferenceName;
  private final List<String> organizers;

  public OrganizerMailComposer(
      JavaMailSender sender, String from, String conferenceName, List<String> organizers) {
    this.sender = sender;
    this.from = from;
    this.conferenceName = conferenceName;
    this.organizers = List.copyOf(organizers);
  }

  public List<MimeMessage> compose(Registration registration) {
    String text = text(registration);
    byte[] copy = FileJsonCopyStore.content(registration);
    List<MimeMessage> messages = new ArrayList<>();
    for (String organizer : organizers) {
      messages.add(message(registration, organizer, text, copy));
    }
    return messages;
  }

  private MimeMessage message(Registration r, String organizer, String text, byte[] copy) {
    try {
      MimeMessage message = sender.createMimeMessage();
      MimeMessageHelper helper =
          new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
      helper.setFrom(from);
      helper.setTo(new InternetAddress(organizer, true));
      helper.setSubject("New registration - " + conferenceName);
      helper.setText(text, false);
      helper.addAttachment(
          "registration-" + r.id() + ".json", new ByteArrayResource(copy), "application/json");
      message.saveChanges();
      return message;
    } catch (MessagingException e) {
      throw new MailPreparationException("organizer notification for " + r.id(), e);
    }
  }

  private static String text(Registration r) {
    StringBuilder text = new StringBuilder("A new registration was received.\n\n");
    line(text, "Registration ID", r.id().toString());
    line(text, "Type", r.type().name());
    line(text, "Registered at", Timestamps.format(r.registeredAt()));
    line(text, "First name", r.firstName());
    line(text, "Last name", r.lastName());
    line(text, "Email", r.email());
    line(text, "Organization / institution", r.organization());
    line(text, "Study institution", r.studyInstitution());
    line(text, "Study programme", r.studyProgramme());
    line(text, "Student ID", r.studentId());
    text.append("\nOptions:\n");
    for (ConferenceOption option : r.options()) {
      text.append("- ").append(option.name()).append(" (").append(option.id()).append(")\n");
    }
    text.append("\nConsents:\n");
    for (GivenConsent consent : r.consents()) {
      text.append("- ")
          .append(consent.id())
          .append(" (")
          .append(Timestamps.format(consent.givenAt()))
          .append(")\n");
    }
    text.append("\nThe registration as stored is attached as JSON.\n");
    return text.toString();
  }

  private static void line(StringBuilder text, String label, String value) {
    if (value != null) {
      text.append(label).append(": ").append(value).append('\n');
    }
  }
}
