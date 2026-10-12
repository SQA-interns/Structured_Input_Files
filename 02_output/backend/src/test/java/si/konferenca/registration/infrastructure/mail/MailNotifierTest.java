package si.konferenca.registration.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import si.konferenca.registration.domain.Category;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.SelectedOption;
import si.konferenca.registration.infrastructure.TestRegistrations;

class MailNotifierTest {

  private final JavaMailSender sender = mock(JavaMailSender.class);
  private final MailNotifier notifier =
      new MailNotifier(sender, "from@x.si", "Conf 2026", List.of("o1@x.si", "o2@x.si"));

  MailNotifierTest() {
    when(sender.createMimeMessage())
        .thenAnswer(i -> new MimeMessage(Session.getInstance(new Properties())));
  }

  private List<MimeMessage> sent() {
    ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(sender, times(3)).send(captor.capture());
    return captor.getAllValues();
  }

  private static List<String> to(MimeMessage m) throws Exception {
    List<String> result = new ArrayList<>();
    for (Address a : m.getRecipients(Message.RecipientType.TO)) {
      result.add(a.toString());
    }
    return result;
  }

  @Test
  void sendsTheParticipantAndOneMessagePerOrganizer() throws Exception {
    Registration r = TestRegistrations.student();

    notifier.registrationAccepted(r, "{\"id\":1}".getBytes());

    List<MimeMessage> messages = sent();
    MimeMessage participant = messages.get(0);
    participant.saveChanges();
    assertThat(to(participant)).containsExactly("luka@example.si");
    assertThat(participant.getSubject()).isEqualTo("Registration confirmation – Conf 2026");
    assertThat(participant.getFrom()[0].toString()).isEqualTo("from@x.si");
    assertThat(participant.getContentType()).startsWith("text/plain").containsIgnoringCase("utf-8");
    assertThat(to(messages.get(1))).containsExactly("o1@x.si");
    assertThat(to(messages.get(2))).containsExactly("o2@x.si");
    MimeMessage organizer = messages.get(1);
    assertThat(organizer.getSubject()).isEqualTo("New registration – Student – Conf 2026");
    assertThat(organizer.getFrom()[0].toString()).isEqualTo("from@x.si");
    String body = null;
    for (int i = 0; i < ((Multipart) organizer.getContent()).getCount(); i++) {
      Part part = ((Multipart) organizer.getContent()).getBodyPart(i);
      if (part.getContent() instanceof Multipart related) {
        body = String.valueOf(related.getBodyPart(0).getContent());
      }
    }
    assertThat(body)
        .isNotNull()
        .contains("Registration ID: 22222222")
        .contains("Last name: Kranjc");
    Multipart mixed = (Multipart) organizer.getContent();
    Part attachment = null;
    for (int i = 0; i < mixed.getCount(); i++) {
      if (Part.ATTACHMENT.equalsIgnoreCase(mixed.getBodyPart(i).getDisposition())) {
        attachment = mixed.getBodyPart(i);
      }
    }
    assertThat(attachment).isNotNull();
    assertThat(attachment.getFileName())
        .isEqualTo("registration-22222222-2222-2222-2222-222222222222.json");
    assertThat(attachment.getInputStream().readAllBytes()).isEqualTo("{\"id\":1}".getBytes());
  }

  @Test
  void participantTextListsOptionsByCategoryOrSaysNone() {
    String text = notifier.participantText(TestRegistrations.student());
    assertThat(text)
        .startsWith("Dear Luka Kranjc,")
        .contains("Registration type: Student")
        .contains("Registration ID: 22222222-2222-2222-2222-222222222222")
        .contains("Workshops: Workshop; Workshop 2\nOther activities: Career fair");
    assertThat(text.indexOf("Workshops")).isLessThan(text.indexOf("Other activities"));

    assertThat(notifier.participantText(TestRegistrations.external("Ana", List.of())))
        .contains("No optional activities selected.");
  }

  @Test
  void organizerTextHasEveryFieldOfTheRegistrationOnly() {
    String external =
        notifier.organizerText(
            TestRegistrations.external(
                "Ana", List.of(new SelectedOption("m", "Lunch", Category.MEAL))));

    assertThat(external)
        .contains("Type: External participant\n")
        .contains("Submitted at: 2026-10-12T10:00:00Z\n")
        .contains("Registration ID: 11111111-1111-1111-1111-111111111111\n")
        .contains("First name: Ana\n")
        .contains("Last name: Novak\n")
        .contains("Email: ana@example.si\n")
        .contains("Organization / institution: Acme\n")
        .contains("Meals: Lunch\n")
        .contains("Consent: I agree.\n")
        .contains("Consent given at: 2026-10-12T10:00:00Z\n")
        .doesNotContain("Student ID");
    assertThat(notifier.organizerText(TestRegistrations.student()))
        .contains("Study institution: Uni\nStudy programme: CS\nStudent ID: 123\n")
        .doesNotContain("Organization");
  }

  @Test
  void aFailedEmailIsSwallowedAndTheOthersAreStillSent() {
    doThrow(new MailSendException("down")).doNothing().when(sender).send(any(MimeMessage.class));

    notifier.registrationAccepted(TestRegistrations.student(), new byte[0]);

    verify(sender, times(3)).send(any(MimeMessage.class));
  }

  @Test
  void anInvalidOrganizerAddressIsSkippedWithoutFailing() {
    MailNotifier broken = new MailNotifier(sender, "from@x.si", "C", List.of("not an address"));

    broken.registrationAccepted(TestRegistrations.student(), new byte[0]);

    verify(sender, times(1)).send(any(MimeMessage.class));
  }
}
