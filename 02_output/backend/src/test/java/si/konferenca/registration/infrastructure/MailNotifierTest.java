package si.konferenca.registration.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;

class MailNotifierTest {

  private final JavaMailSender sender = mock(JavaMailSender.class);
  private final MailNotifier notifier =
      new MailNotifier(
          sender,
          new MailNotifier.MailSettings(
              "from@konf.si", "Konf\r\n2026", List.of(" o1@konf.si ", "", "o2@konf.si")));

  private MimeMessage sent() {
    ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(sender).send(captor.capture());
    return captor.getValue();
  }

  @Test
  void settingsCleanConfigurationValues() {
    MailNotifier.MailSettings settings =
        new MailNotifier.MailSettings("f", "A\nB", List.of(" x ", " "));

    assertThat(settings.conferenceName()).isEqualTo("A B");
    assertThat(settings.organizerEmails()).containsExactly("x");
  }

  @Test
  void participantConfirmationIsPlainText() throws Exception {
    when(sender.createMimeMessage())
        .thenReturn(new MimeMessage(Session.getInstance(new Properties())));

    notifier.sendParticipantConfirmation(Registrations.external());

    MimeMessage message = sent();
    assertThat(message.getFrom()[0].toString()).isEqualTo("from@konf.si");
    assertThat(message.getAllRecipients())
        .extracting(Object::toString)
        .containsExactly("c@example.si");
    assertThat(message.getSubject())
        .isEqualTo("Konf  2026 – registration confirmed (" + Registrations.REFERENCE + ")");
    assertThat(message.getContentType()).startsWith("text/plain");
    assertThat((String) message.getContent())
        .contains("Dear Čedomir Žagar,", "Registration type: External participant")
        .contains("- AI (Workshop)", "- Lunch (Meal)");
  }

  @Test
  void organizerNotificationAttachesTheCopy() throws Exception {
    when(sender.createMimeMessage())
        .thenReturn(new MimeMessage(Session.getInstance(new Properties())));
    byte[] copy = "{\"x\":1}".getBytes();

    notifier.sendOrganizerNotification(Registrations.student(), copy);

    MimeMessage message = sent();
    message.saveChanges();
    assertThat(message.getAllRecipients())
        .extracting(Object::toString)
        .containsExactly("o1@konf.si", "o2@konf.si");
    assertThat(message.getSubject())
        .isEqualTo("Konf  2026 – new registration " + Registrations.REFERENCE);
    MimeMultipart mixed = (MimeMultipart) message.getContent();
    assertThat(attachment(mixed).getFileName())
        .isEqualTo("registration-" + Registrations.REFERENCE + ".json");
    assertThat(attachment(mixed).getInputStream().readAllBytes()).isEqualTo(copy);
  }

  private static jakarta.mail.BodyPart attachment(MimeMultipart mixed) throws MessagingException {
    return mixed.getBodyPart(mixed.getCount() - 1);
  }

  @Test
  void composeFailureBecomesIllegalState() throws Exception {
    MimeMessage broken = mock(MimeMessage.class);
    when(sender.createMimeMessage()).thenReturn(broken);
    doThrow(new MessagingException("x")).when(broken).setFrom(any(jakarta.mail.Address.class));

    assertThatThrownBy(() -> notifier.sendParticipantConfirmation(Registrations.external()))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () -> notifier.sendOrganizerNotification(Registrations.external(), new byte[0]))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void organizerTextListsRegistrationData() throws IOException {
    String text = MailTexts.organizerNotification(Registrations.student());

    assertThat(text)
        .contains(
            "New registration "
                + Registrations.REFERENCE
                + " submitted at 2026-10-11T08:00:00.123Z.")
        .contains("Type: Student", "First name: Luka", "Last name: Kranjc", "Email: l@example.si")
        .contains("Study institution: UM", "Study programme: RIT", "Student ID: E1")
        .contains("Workshops: -", "Events: -", "Meals: -", "Other activities: -")
        .contains("- data given at 2026-10-11T08:00:00.123Z", "- photos given at")
        .doesNotContain("Organization");
    assertThat(MailTexts.organizerNotification(Registrations.external()))
        .contains("Organization / institution: IJS", "Workshops: AI, Security", "Meals: Lunch")
        .doesNotContain("Study institution");
  }

  @Test
  void participantTextWithoutOptions() {
    assertThat(MailTexts.participantConfirmation(Registrations.student(), "Konf"))
        .contains(
            "your registration for Konf has been received.",
            "Registration type: Student",
            "- none");
  }
}
