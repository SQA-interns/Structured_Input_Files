package si.konferenca.registration.adapter.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import si.konferenca.registration.adapter.Fixtures;
import si.konferenca.registration.domain.Registration;

class MailRegistrationNotifierTest {

  private final JavaMailSender sender = mock(JavaMailSender.class);
  private MailRegistrationNotifier notifier;

  private MailRegistrationNotifier create() throws Exception {
    when(sender.createMimeMessage())
        .thenAnswer(i -> new MimeMessage(Session.getInstance(new Properties())));
    notifier =
        new MailRegistrationNotifier(
            sender,
            "registration@konferenca.si",
            "Konferenca 2026",
            List.of(new InternetAddress("o1@example.si"), new InternetAddress("o2@example.si")));
    return notifier;
  }

  @AfterEach
  void close() {
    if (notifier != null) {
      notifier.close();
    }
  }

  @Test
  void participantMessageIsPlainTextToTheParticipantOnly() throws Exception {
    MimeMessage m = create().participantMessage(Fixtures.external());
    m.saveChanges();

    assertThat(m.getRecipients(Message.RecipientType.TO))
        .extracting(Object::toString)
        .containsExactly("crt@example.si");
    assertThat(m.getRecipients(Message.RecipientType.CC)).isNull();
    assertThat(m.getSubject()).isEqualTo("Registration confirmation – Konferenca 2026");
    assertThat(m.getFrom()[0].toString()).isEqualTo("registration@konferenca.si");
    assertThat(m.getContentType()).startsWith("text/plain");
    assertThat((String) m.getContent()).contains("Dear Črt Žagar,").contains("Meal: Kosilo");
  }

  @Test
  void organizerMessageGoesToAllOrganizersWithTheJsonAttached() throws Exception {
    byte[] copy = "{\"id\":1}".getBytes();
    MimeMessage m = create().organizerMessage(Fixtures.external(), copy);
    m.saveChanges();

    assertThat(m.getRecipients(Message.RecipientType.TO))
        .extracting(Object::toString)
        .containsExactly("o1@example.si", "o2@example.si");
    assertThat(m.getSubject()).isEqualTo("New registration – Konferenca 2026");
    Multipart mixed = (Multipart) m.getContent();
    Part attachment = null;
    for (int i = 0; i < mixed.getCount(); i++) {
      if (Part.ATTACHMENT.equalsIgnoreCase(mixed.getBodyPart(i).getDisposition())) {
        attachment = mixed.getBodyPart(i);
      }
    }
    assertThat(attachment).isNotNull();
    assertThat(attachment.getFileName())
        .isEqualTo("registration-11111111-2222-3333-4444-555555555555.json");
    assertThat(attachment.getContentType()).startsWith("application/json");
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    attachment.getDataHandler().writeTo(out);
    assertThat(out.toByteArray()).isEqualTo(copy);
  }

  @Test
  void sendsBothMessagesAndSurvivesFailures() throws Exception {
    create();
    doThrow(new MailSendException("down")).doNothing().when(sender).send(any(MimeMessage.class));

    notifier.registrationAccepted(Fixtures.student(), new byte[] {1});

    ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
    verify(sender, timeout(5000).times(2)).send(sent.capture());
    assertThat(sent.getAllValues().get(1).getSubject()).startsWith("New registration");
  }

  @Test
  void textsFollowTheContract() {
    Registration r = Fixtures.external();
    String participant = MailTexts.participant(r, "Konferenca 2026");
    String organizer = MailTexts.organizer(r);

    assertThat(participant)
        .contains("your registration for Konferenca 2026 has been received.")
        .contains("Registration ID: " + r.id())
        .contains("Registration type: External participant")
        .contains("- Workshop: Delavnica")
        .endsWith("Konferenca 2026 organizers\n");
    assertThat(organizer)
        .contains("Registered at: 2026-10-11T08:30:00Z")
        .contains("Organization / institution: IJS")
        .doesNotContain("Student ID")
        .contains("- privacy at 2026-10-11T08:30:00Z");
    assertThat(MailTexts.participant(Fixtures.student(), "K")).contains("- none");
    assertThat(MailTexts.organizer(Fixtures.student()))
        .contains("Student ID: 632")
        .doesNotContain("Organization");
    verify(sender, times(0)).send(any(MimeMessage.class));
  }

  @Test
  void closeStopsTheBackgroundExecutor() throws Exception {
    create().close();

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> notifier.registrationAccepted(Fixtures.student(), new byte[0]))
        .isInstanceOf(java.util.concurrent.RejectedExecutionException.class);
  }
}
