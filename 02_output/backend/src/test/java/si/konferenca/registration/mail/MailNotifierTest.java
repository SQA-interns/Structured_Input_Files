package si.konferenca.registration.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import si.konferenca.registration.RegistrationProperties;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;

class MailNotifierTest {

  static final UUID ID = UUID.fromString("6f1c1b7e-2f7a-4c55-9d0e-3c1f7c2b9a10");
  static final Instant AT = Instant.parse("2026-10-13T08:00:00.120Z");

  static Registration external(String email, List<Registration.SelectedOption> options) {
    return new Registration(
        ID,
        AT,
        RegistrationType.EXTERNAL,
        "Ana",
        "Novak",
        email,
        "Acme",
        null,
        null,
        null,
        options,
        List.of(new Registration.GivenConsent("privacy", "I agree", AT)));
  }

  static Registration student() {
    return new Registration(
        ID,
        AT,
        RegistrationType.STUDENT,
        "Bor",
        "Kranjc",
        "bor@example.com",
        null,
        "Uni",
        "CS",
        "123",
        List.of(),
        List.of());
  }

  static final List<Registration.SelectedOption> OPTIONS =
      List.of(
          new Registration.SelectedOption("ws", "Workshop A", OptionCategory.WORKSHOP),
          new Registration.SelectedOption("ot", "Tour", OptionCategory.OTHER));

  private final JavaMailSender sender = mock(JavaMailSender.class);

  private MailNotifier notifier(List<String> organizers) {
    when(sender.createMimeMessage())
        .thenAnswer(i -> new MimeMessage(Session.getInstance(new Properties())));
    return new MailNotifier(
        sender,
        new RegistrationProperties(
            "Konferenca 2026",
            "c",
            Path.of("x"),
            new RegistrationProperties.Mail("registration@konferenca.si", organizers),
            new RegistrationProperties.Recaptcha(true, "", "", URI.create("http://x")),
            new RegistrationProperties.Organizer("u", "p", true),
            new RegistrationProperties.Cors(List.of()),
            new RegistrationProperties.RateLimit(1, 1, 1),
            100));
  }

  private List<MimeMessage> sent(int count) {
    ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(sender, times(count)).send(captor.capture());
    return captor.getAllValues();
  }

  private static List<String> addresses(Address[] addresses) {
    return addresses == null ? List.of() : Arrays.stream(addresses).map(Address::toString).toList();
  }

  @Test
  void participantGetsOnePlainTextMessage() throws Exception {
    notifier(List.of()).notifyParticipant(external("ana@example.com", OPTIONS));

    MimeMessage message = sent(1).getFirst();
    message.saveChanges();
    assertThat(addresses(message.getRecipients(Message.RecipientType.TO)))
        .containsExactly("ana@example.com");
    assertThat(addresses(message.getRecipients(Message.RecipientType.CC))).isEmpty();
    assertThat(addresses(message.getRecipients(Message.RecipientType.BCC))).isEmpty();
    assertThat(addresses(message.getFrom())).containsExactly("registration@konferenca.si");
    assertThat(message.getSubject()).isEqualTo("Registration confirmed: Konferenca 2026");
    assertThat(message.getContentType()).startsWith("text/plain").containsIgnoringCase("UTF-8");
    assertThat(message.getContent())
        .isEqualTo(
            EmailTexts.participantBody(external("ana@example.com", OPTIONS), "Konferenca 2026"));
  }

  @Test
  void eachOrganizerGetsOwnMessageWithBase64JsonAttachment() throws Exception {
    byte[] copy = "{\n  \"id\" : \"x\"\n}".getBytes(StandardCharsets.UTF_8);

    notifier(List.of("one@konferenca.si", "two@konferenca.si"))
        .notifyOrganizers(external("ana@example.com", OPTIONS), copy);

    List<MimeMessage> messages = sent(2);
    assertThat(addresses(messages.get(0).getRecipients(Message.RecipientType.TO)))
        .containsExactly("one@konferenca.si");
    assertThat(addresses(messages.get(1).getRecipients(Message.RecipientType.TO)))
        .containsExactly("two@konferenca.si");
    MimeMessage message = messages.get(0);
    message.saveChanges();
    assertThat(message.getSubject()).isEqualTo("New registration: Konferenca 2026");
    MimeMultipart mixed = (MimeMultipart) message.getContent();
    assertThat(mixed.getContentType()).startsWith("multipart/mixed");
    MimeBodyPart attachment = (MimeBodyPart) mixed.getBodyPart(mixed.getCount() - 1);
    assertThat(attachment.getFileName()).isEqualTo("registration-" + ID + ".json");
    assertThat(attachment.getContentType()).startsWith("application/json");
    assertThat(attachment.getDisposition()).isEqualTo("attachment");
    assertThat(attachment.getEncoding()).isEqualTo("base64");
    assertThat(attachment.getInputStream().readAllBytes()).isEqualTo(copy);
    String text =
        new String(
            ((MimeBodyPart) mixed.getBodyPart(0)).getInputStream().readAllBytes(),
            StandardCharsets.UTF_8);
    assertThat(text).contains("Registration ID: " + ID);
  }

  @Test
  void noOrganizersMeansNoMessage() {
    notifier(List.of()).notifyOrganizers(external("ana@example.com", OPTIONS), new byte[0]);
    verify(sender, times(0)).send(any(MimeMessage.class));
  }

  @Test
  void sendFailureIsSwallowedAndTheNextOrganizerStillGetsMail() {
    MailNotifier notifier = notifier(List.of("one@konferenca.si", "two@konferenca.si"));
    doThrow(new MailSendException("down")).doNothing().when(sender).send(any(MimeMessage.class));

    notifier.notifyOrganizers(external("ana@example.com", OPTIONS), new byte[] {1});

    verify(sender, times(2)).send(any(MimeMessage.class));
  }

  @Test
  void invalidOrMultipleRecipientAddressesAreNotSent() {
    MailNotifier notifier = notifier(List.of());

    notifier.notifyParticipant(external("a@b.co, c@d.co", OPTIONS));
    notifier.notifyParticipant(external("Ana <a@b.co>", OPTIONS));
    notifier.notifyParticipant(external("a@b@c.co", OPTIONS));

    verify(sender, times(0)).send(any(MimeMessage.class));
  }

  @Test
  void participantBodyTemplate() {
    assertThat(EmailTexts.participantBody(external("ana@example.com", OPTIONS), "Konf"))
        .isEqualTo(
            "Dear Ana Novak,\n\n"
                + "thank you for registering for Konf. Your registration has been received.\n\n"
                + "Registration type: External participant\n"
                + "Selected options:\n"
                + "- Workshop A (Workshop)\n"
                + "- Tour (Other activity)\n"
                + "\nRegistration ID: "
                + ID
                + "\n");
    assertThat(EmailTexts.participantBody(student(), "Konf"))
        .contains("Registration type: Student\nSelected options:\n- none\n");
  }

  @Test
  void organizerBodyTemplate() {
    assertThat(EmailTexts.organizerBody(external("ana@example.com", OPTIONS), "Konf"))
        .isEqualTo(
            "A new registration was received for Konf.\n\n"
                + "Registration ID: "
                + ID
                + "\n"
                + "Registered at: 2026-10-13T08:00:00.120Z\n"
                + "Type: EXTERNAL\n"
                + "First name: Ana\n"
                + "Last name: Novak\n"
                + "Email: ana@example.com\n"
                + "Organization / institution: Acme\n"
                + "Options:\n"
                + "- Workshop A (Workshop)\n"
                + "- Tour (Other activity)\n"
                + "Consents:\n"
                + "- privacy: given 2026-10-13T08:00:00.120Z\n"
                + "\nThe raw registration is attached as JSON.\n");
    assertThat(EmailTexts.organizerBody(student(), "Konf"))
        .contains(
            "Study institution: Uni\nStudy programme: CS\nStudent ID: 123\nOptions:\n- none\n")
        .doesNotContain("Organization")
        .endsWith("Consents:\n\nThe raw registration is attached as JSON.\n");
    assertThat(EmailTexts.participantSubject("K")).isEqualTo("Registration confirmed: K");
    assertThat(EmailTexts.organizerSubject("K")).isEqualTo("New registration: K");
  }
}
