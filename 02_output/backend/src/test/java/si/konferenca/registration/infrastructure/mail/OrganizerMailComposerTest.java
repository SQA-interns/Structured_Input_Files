package si.konferenca.registration.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;
import si.konferenca.registration.infrastructure.file.FileJsonCopyStore;

/** Covers AC-007-01, AC-007-02. */
class OrganizerMailComposerTest {

  static final Instant TIME = Instant.parse("2026-10-19T12:00:00.123Z");

  static Registration student() {
    return new Registration(
        UUID.fromString("00000000-0000-0000-0000-000000000077"),
        RegistrationType.STUDENT,
        TIME,
        "Luka",
        "Kranjc Č",
        "luka@example.com",
        null,
        "Univerza v Ljubljani",
        "Računalništvo",
        "6321",
        List.of(new ConferenceOption("ws-a", "Workshop A", OptionCategory.WORKSHOP, true, null)),
        List.of(new GivenConsent("privacy", TIME)));
  }

  final OrganizerMailComposer composer =
      new OrganizerMailComposer(
          new JavaMailSenderImpl(),
          "registration@konferenca.test",
          "Konferenca 2026",
          List.of("one@konferenca.test", "two@konferenca.test"));

  @Test
  void AC_007_01_oneNotificationPerOrganizerWithTheRegistrationData() throws Exception {
    List<MimeMessage> messages = composer.compose(student());

    assertThat(messages).hasSize(2);
    assertThat(messages.get(0).getRecipients(Message.RecipientType.TO))
        .containsExactly(new InternetAddress("one@konferenca.test"));
    assertThat(messages.get(1).getRecipients(Message.RecipientType.TO))
        .containsExactly(new InternetAddress("two@konferenca.test"));
    for (MimeMessage message : messages) {
      assertThat(message.getRecipients(Message.RecipientType.CC)).isNull();
      assertThat(message.getRecipients(Message.RecipientType.BCC)).isNull();
      assertThat(message.getSubject()).isEqualTo("New registration - Konferenca 2026");
      assertThat(text(message))
          .contains(
              "00000000-0000-0000-0000-000000000077",
              "STUDENT",
              "Luka",
              "Kranjc Č",
              "luka@example.com",
              "Univerza v Ljubljani",
              "Računalništvo",
              "6321",
              "Workshop A (ws-a)",
              "privacy (2026-10-19T12:00:00.123Z)");
    }
  }

  @Test
  void AC_007_02_notificationAttachesTheJsonCopyBytes() throws Exception {
    MimeMessage message = composer.compose(student()).get(0);

    BodyPart attachment = attachment(message);
    assertThat(attachment.getFileName())
        .isEqualTo("registration-00000000-0000-0000-0000-000000000077.json");
    assertThat(attachment.getContentType()).startsWith("application/json");
    assertThat(bytes(attachment)).isEqualTo(FileJsonCopyStore.content(student()));
  }

  static String text(MimeMessage message) throws Exception {
    List<String> texts = new ArrayList<>();
    collectText(message, texts);
    return String.join("\n", texts);
  }

  private static void collectText(Part part, List<String> texts) throws Exception {
    if (part.isMimeType("text/plain") && !Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
      texts.add((String) part.getContent());
    } else if (part.isMimeType("multipart/*")) {
      Multipart multipart = (Multipart) part.getContent();
      for (int i = 0; i < multipart.getCount(); i++) {
        collectText(multipart.getBodyPart(i), texts);
      }
    }
  }

  static BodyPart attachment(MimeMessage message) throws Exception {
    Multipart multipart = (Multipart) message.getContent();
    for (int i = 0; i < multipart.getCount(); i++) {
      BodyPart part = multipart.getBodyPart(i);
      if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
        return part;
      }
    }
    throw new AssertionError("no attachment");
  }

  static byte[] bytes(BodyPart part) throws Exception {
    try (InputStream in = part.getInputStream()) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      in.transferTo(out);
      return out.toByteArray();
    }
  }
}
