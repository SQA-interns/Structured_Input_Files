package si.konferenca.registration.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;

/** Covers AC-006-01, AC-006-02. */
class ParticipantMailComposerTest {

  static final Instant TIME = Instant.parse("2026-10-19T12:00:00.123Z");

  static Registration registration(
      String firstName, String lastName, List<ConferenceOption> options) {
    return new Registration(
        UUID.fromString("00000000-0000-0000-0000-000000000042"),
        RegistrationType.EXTERNAL,
        TIME,
        firstName,
        lastName,
        "ana@example.com",
        "IJS",
        null,
        null,
        null,
        options,
        List.of(new GivenConsent("privacy", TIME)));
  }

  static final List<ConferenceOption> OPTIONS =
      List.of(
          new ConferenceOption("ws-a", "Delavnica Čšž", OptionCategory.WORKSHOP, true, null),
          new ConferenceOption("meal", "Lunch", OptionCategory.MEAL, true, null));

  final ParticipantMailComposer composer =
      new ParticipantMailComposer(
          new JavaMailSenderImpl(), "registration@konferenca.test", "Konferenca 2026");

  @Test
  void AC_006_01_confirmationNamesConferenceParticipantAndOptions() throws Exception {
    MimeMessage message = composer.compose(registration("Ana", "Novak", OPTIONS));

    assertThat(message.getRecipients(Message.RecipientType.TO))
        .containsExactly(new InternetAddress("ana@example.com"));
    assertThat(message.getFrom())
        .containsExactly(new InternetAddress("registration@konferenca.test"));
    assertThat(message.getSubject()).isEqualTo("Registration confirmation - Konferenca 2026");
    assertThat(message.getContentType()).startsWith("text/plain").containsIgnoringCase("UTF-8");
    String text = (String) message.getContent();
    assertThat(text).contains("Konferenca 2026", "Ana", "Novak", "Delavnica Čšž", "Lunch");
  }

  @Test
  void AC_006_01_confirmationWithoutOptionsSaysNone() throws Exception {
    String text = (String) composer.compose(registration("Ana", "Novak", List.of())).getContent();

    assertThat(text).contains("none");
  }

  @Test
  void AC_006_02_inputCannotAddHeadersRecipientsOrMarkup() throws Exception {
    MimeMessage message =
        composer.compose(
            registration(
                "Ana\r\nBcc: intruder@evil.example\r\nSubject: hacked",
                "<b>Novak</b><script>x</script>",
                OPTIONS));
    message.saveChanges();

    assertThat(message.getRecipients(Message.RecipientType.TO)).hasSize(1);
    assertThat(message.getRecipients(Message.RecipientType.CC)).isNull();
    assertThat(message.getRecipients(Message.RecipientType.BCC)).isNull();
    assertThat(message.getSubject()).isEqualTo("Registration confirmation - Konferenca 2026");
    assertThat(message.getHeader("Bcc")).isNull();
    assertThat(message.getContentType()).startsWith("text/plain");
    assertThat((String) message.getContent()).contains("<b>Novak</b><script>x</script>");
  }
}
