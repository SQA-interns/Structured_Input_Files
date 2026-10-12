package si.konferenca.registration.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;
import si.konferenca.registration.config.AppProperties;
import si.konferenca.registration.domain.ParticipantDetails;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;

class MailNotifierTest {

  private final JavaMailSender sender = mock(JavaMailSender.class);
  private final List<MimeMessage> sent = new ArrayList<>();

  private MailNotifier notifier(List<String> organizers) throws Exception {
    doAnswer(
            invocation -> {
              MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
              ((MimeMessagePreparator) invocation.getArgument(0)).prepare(message);
              message.saveChanges();
              sent.add(message);
              return null;
            })
        .when(sender)
        .send(any(MimeMessagePreparator.class));
    return new MailNotifier(
        sender,
        new AppProperties(
            "test",
            new AppProperties.Conference("classpath:conference.json", "Konferenca 2026"),
            new AppProperties.JsonCopy(Path.of("copies")),
            new AppProperties.Recaptcha(true, "", "", "http://localhost"),
            new AppProperties.Organizer("o", "p", organizers, true),
            new AppProperties.Mail("registration@konferenca.si"),
            new AppProperties.Limits(1, 1, 1, 100),
            new AppProperties.Cors("")));
  }

  @Test
  void participantConfirmationIsPlainTextToTheParticipantOnly() throws Exception {
    notifier(List.of("org@example.org")).sendParticipantConfirmation(Registrations.external());

    assertThat(sent).hasSize(1);
    MimeMessage message = sent.getFirst();
    assertThat(message.getAllRecipients())
        .extracting(Object::toString)
        .containsExactly("ana@example.org");
    assertThat(message.getFrom())
        .extracting(Object::toString)
        .containsExactly("registration@konferenca.si");
    assertThat(message.getSubject()).isEqualTo("Registration confirmed: Konferenca 2026");
    assertThat(message.getContentType()).startsWith("text/plain").contains("UTF-8");
    assertThat((String) message.getContent())
        .isEqualTo(
            "Dear Ana Novak,\n\nthank you for registering for Konferenca 2026.\n\n"
                + "Registration type: External participant\n\n"
                + "Selected options:\n- Workshop A\n- Lunch\n\n"
                + "Registration ID: "
                + Registrations.EXTERNAL_ID
                + "\n\nKonferenca 2026\n");
  }

  @Test
  void participantTextNamesStudentsAndMissingOptions() throws Exception {
    String text = notifier(List.of("org@example.org")).participantText(Registrations.student());

    assertThat(text).contains("Registration type: Student").contains("Selected options: none\n");
  }

  @Test
  void organizerNotificationGoesToEachAddressWithTheCopyAttached() throws Exception {
    byte[] copy = "{\n  \"id\": \"x\"\n}\n".getBytes(StandardCharsets.UTF_8);

    int failed =
        notifier(List.of(" one@example.org", "two@example.org "))
            .sendOrganizerNotifications(Registrations.student(), copy);

    assertThat(failed).isZero();
    assertThat(sent).hasSize(2);
    assertThat(sent.get(0).getAllRecipients())
        .extracting(Object::toString)
        .containsExactly("one@example.org");
    assertThat(sent.get(1).getAllRecipients())
        .extracting(Object::toString)
        .containsExactly("two@example.org");
    MimeMessage message = sent.getFirst();
    assertThat(message.getSubject()).isEqualTo("New registration " + Registrations.STUDENT_ID);
    Multipart mixed = (Multipart) message.getContent();
    BodyPart attachment = findAttachment(mixed);
    assertThat(attachment.getFileName())
        .isEqualTo("registration-" + Registrations.STUDENT_ID + ".json");
    assertThat(attachment.getContentType()).startsWith("application/json");
    assertThat(((MimeBodyPart) attachment).getEncoding()).isEqualTo("base64");
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    attachment.getDataHandler().writeTo(bytes);
    assertThat(bytes.toByteArray()).isEqualTo(copy);
  }

  private static BodyPart findAttachment(Multipart multipart) throws Exception {
    for (int i = 0; i < multipart.getCount(); i++) {
      BodyPart part = multipart.getBodyPart(i);
      if (BodyPart.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
        return part;
      }
      if (part.getContent() instanceof Multipart nested) {
        BodyPart found = findAttachment(nested);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  @Test
  void organizerTextListsAllSubmittedData() throws Exception {
    MailNotifier notifier = notifier(List.of("org@example.org"));

    String external = notifier.organizerText(Registrations.external());
    String student = notifier.organizerText(Registrations.student());

    assertThat(external)
        .isEqualTo(
            "New registration for Konferenca 2026\n\nRegistration ID: "
                + Registrations.EXTERNAL_ID
                + "\nType: EXTERNAL\nRegistered at: 2026-10-12T10:15:30.123456Z\n"
                + "First name: Ana\nLast name: Novak\nEmail: ana@example.org\n"
                + "Organization: Institut\n\nSelected options:\n- Workshop A\n- Lunch\n\n"
                + "Consents:\n- privacy (given at 2026-10-12T10:15:30.123456Z)\n"
                + "- photo (given at 2026-10-12T10:15:30.123456Z)\n\n"
                + "The registration as accepted is attached as JSON.\n");
    assertThat(student)
        .contains("Study institution: Univerza\n")
        .contains("Study programme: CS\n")
        .contains("Student ID: 6320\n")
        .doesNotContain("Organization:");
  }

  @Test
  void countsOrganizerAddressesThatFail() throws Exception {
    MailNotifier notifier = notifier(List.of("one@example.org", "two@example.org"));
    doThrow(new MailSendException("down")).when(sender).send(any(MimeMessagePreparator.class));

    assertThat(notifier.sendOrganizerNotifications(Registrations.external(), new byte[] {1}))
        .isEqualTo(2);
  }

  @Test
  void headerInjectionInNamesStaysInTheBody() throws Exception {
    Registration registration =
        new Registration(
            Registrations.EXTERNAL_ID,
            RegistrationType.EXTERNAL,
            new ParticipantDetails(
                "Eve\r\nBcc: x@evil.example",
                "<b>B</b>",
                "eve@example.org",
                "Org",
                null,
                null,
                null),
            Registrations.AT);

    notifier(List.of("org@example.org")).sendParticipantConfirmation(registration);

    MimeMessage message = sent.getFirst();
    assertThat(message.getAllRecipients()).hasSize(1);
    assertThat(message.getHeader("Bcc")).isNull();
    assertThat((String) message.getContent()).contains("<b>B</b>");
  }
}
