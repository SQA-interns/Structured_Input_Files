package si.konferenca.registration.adapter.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import si.konferenca.registration.TestRegistrations;
import si.konferenca.registration.domain.Registration;

/** US-006, US-007, SR-05: messages built for the SMTP server, captured instead of sent. */
class MailRegistrationNotifierTest {

  /** Captures messages; fails for the addresses listed in {@code failFor}. */
  static final class CapturingSender extends JavaMailSenderImpl {
    final List<MimeMessage> sent = new ArrayList<>();
    final List<String> failFor = new ArrayList<>();

    @Override
    public void send(MimeMessage message) {
      try {
        String to = message.getRecipients(Message.RecipientType.TO)[0].toString();
        if (failFor.contains(to)) {
          throw new MailSendException("unavailable");
        }
      } catch (jakarta.mail.MessagingException e) {
        throw new IllegalStateException(e);
      }
      try {
        message.saveChanges();
      } catch (jakarta.mail.MessagingException e) {
        throw new IllegalStateException(e);
      }
      sent.add(message);
    }
  }

  private final CapturingSender sender = new CapturingSender();

  private MailRegistrationNotifier notifier(String organizers) {
    return new MailRegistrationNotifier(sender, "from@konferenca.si", organizers, "Konf 26");
  }

  private static String text(Part part) throws Exception {
    Object content = part.getContent();
    if (content instanceof String s) {
      return s;
    }
    Multipart multipart = (Multipart) content;
    return text(multipart.getBodyPart(0));
  }

  @Test
  void confirmationGoesOnlyToTheParticipantAsPlainText() throws Exception {
    Registration r = TestRegistrations.external();

    notifier("org@konferenca.si").sendConfirmation(r);

    MimeMessage message = sender.sent.get(0);
    assertThat(message.getAllRecipients())
        .extracting(Address::toString)
        .containsExactly("ziga@example.si");
    assertThat(message.getFrom())
        .extracting(Address::toString)
        .containsExactly("from@konferenca.si");
    assertThat(message.getSubject()).isEqualTo("Registration confirmation – Konf 26");
    assertThat(message.getContentType()).startsWith("text/plain").containsIgnoringCase("UTF-8");
    assertThat(text(message)).contains("Dear Žiga Čeč,", "Organization / institution: Šola <b>");
  }

  @Test
  void eachOrganizerGetsOneNotificationWithTheJsonCopyAttached() throws Exception {
    Registration r = TestRegistrations.external();
    byte[] copy = "{\"id\":\"x\",\"name\":\"Žiga\"}".getBytes(StandardCharsets.UTF_8);

    notifier(" a@konferenca.si , b@konferenca.si ,").notifyOrganizers(r, copy);

    assertThat(sender.sent).hasSize(2);
    assertThat(sender.sent.get(1).getAllRecipients())
        .extracting(Address::toString)
        .containsExactly("b@konferenca.si");
    MimeMessage message = sender.sent.get(0);
    assertThat(message.getSubject()).isEqualTo("New registration – Konf 26");
    assertThat(text(message))
        .contains("A new registration was received.", "Email: ziga@example.si");
    Multipart multipart = (Multipart) message.getContent();
    Part attachment = null;
    for (int i = 0; i < multipart.getCount(); i++) {
      if (Part.ATTACHMENT.equalsIgnoreCase(multipart.getBodyPart(i).getDisposition())) {
        attachment = multipart.getBodyPart(i);
      }
    }
    assertThat(attachment).isNotNull();
    assertThat(attachment.getFileName()).isEqualTo("registration-" + r.id() + ".json");
    assertThat(attachment.getContentType()).startsWith("application/json");
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    attachment.getDataHandler().writeTo(out);
    assertThat(out.toByteArray()).isEqualTo(copy);
  }

  @Test
  void aFailingOrganizerAddressDoesNotStopTheOthers() {
    sender.failFor.add("a@konferenca.si");

    notifier("a@konferenca.si,b@konferenca.si")
        .notifyOrganizers(TestRegistrations.student(), new byte[] {'{', '}'});

    assertThat(sender.sent).hasSize(1);
  }

  @Test
  void confirmationFailurePropagatesToTheCaller() {
    sender.failFor.add("luka@example.si");
    MailRegistrationNotifier n = notifier("a@konferenca.si");
    assertThatThrownBy(() -> n.sendConfirmation(TestRegistrations.student()))
        .isInstanceOf(MailSendException.class);
  }

  @Test
  void organizerRecipientsMustBeValidAndPresent() {
    assertThat(MailRegistrationNotifier.parseRecipients("a@b.si, c@d.si"))
        .containsExactly("a@b.si", "c@d.si");
    assertThatThrownBy(() -> MailRegistrationNotifier.parseRecipients(" , "))
        .hasMessageContaining("at least one");
    assertThatThrownBy(() -> MailRegistrationNotifier.parseRecipients("a@b.si,not-an-address"))
        .hasMessageContaining("invalid address");
  }

  @Test
  void texts() {
    String student = MailTexts.confirmation(TestRegistrations.student(), "Konf");
    assertThat(student)
        .contains("Registration type: Student", "Study institution: Uni")
        .contains("Study programme: CS", "Student ID: 63", "Selected options:\n- none\n")
        .endsWith("Kind regards,\nKonf organizers\n");
    String organizer = MailTexts.organizerNotification(TestRegistrations.external());
    assertThat(organizer)
        .contains("Registration ID: 0b8f3c1e-2d4a-4b6e-9f00-112233445566\n")
        .contains("Registered at: 2026-10-13T20:15:03.120Z\n", "- Workshop (Workshop)\n")
        .contains("- Event (Event)\n", "Consents:\n- privacy: given at 2026-10-13T20:15:03.120Z\n");
  }
}
