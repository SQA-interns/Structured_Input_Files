package si.konferenca.registration.adapter.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationNotifier;

/**
 * Plain-text emails sent in the background after commit (contract email.md). User input only
 * appears in the body (SR-05); failures are logged with the registration id only (D-07, ES-07).
 */
public final class MailRegistrationNotifier implements RegistrationNotifier, AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(MailRegistrationNotifier.class);

  private final JavaMailSender sender;
  private final String from;
  private final String conferenceName;
  private final List<InternetAddress> organizers;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  public MailRegistrationNotifier(
      JavaMailSender sender, String from, String conferenceName, List<InternetAddress> organizers) {
    this.sender = sender;
    this.from = from;
    this.conferenceName = conferenceName;
    this.organizers = List.copyOf(organizers);
  }

  @Override
  public void registrationAccepted(Registration registration, byte[] jsonCopy) {
    executor.execute(
        () -> {
          send(registration, "participant", () -> participantMessage(registration));
          send(registration, "organizer", () -> organizerMessage(registration, jsonCopy));
        });
  }

  private void send(Registration r, String kind, MessageFactory factory) {
    try {
      sender.send(factory.create());
    } catch (MessagingException | RuntimeException e) {
      LOG.warn("Could not send {} email for registration {}", kind, r.id());
    }
  }

  MimeMessage participantMessage(Registration r) throws MessagingException {
    MimeMessage message = sender.createMimeMessage();
    MimeMessageHelper h = new MimeMessageHelper(message, false, "UTF-8");
    h.setFrom(from);
    h.setTo(new InternetAddress(r.participant().email(), true));
    h.setSubject("Registration confirmation – " + conferenceName);
    h.setText(MailTexts.participant(r, conferenceName), false);
    return message;
  }

  MimeMessage organizerMessage(Registration r, byte[] jsonCopy) throws MessagingException {
    MimeMessage message = sender.createMimeMessage();
    MimeMessageHelper h = new MimeMessageHelper(message, true, "UTF-8");
    h.setFrom(from);
    h.setTo(organizers.toArray(InternetAddress[]::new));
    h.setSubject("New registration – " + conferenceName);
    h.setText(MailTexts.organizer(r), false);
    h.addAttachment(
        "registration-" + r.id() + ".json", new ByteArrayResource(jsonCopy), "application/json");
    return message;
  }

  @Override
  public void close() {
    executor.shutdown();
  }

  @FunctionalInterface
  private interface MessageFactory {
    MimeMessage create() throws MessagingException;
  }
}
