package si.konferenca.registration.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/** Covers AC-007-01: sender and registration time of the notification (phase 5 survivors). */
class OrganizerMailDetailsTest {

  @Test
  void AC_007_01_notificationIsSentFromTheConfiguredAddressWithTheRegistrationTime()
      throws Exception {
    MimeMessage message =
        new OrganizerMailComposer(
                new JavaMailSenderImpl(), "from@konferenca.test", "Konf", List.of("o@k.si"))
            .compose(OrganizerMailComposerTest.student())
            .get(0);

    assertThat(message.getFrom()).containsExactly(new InternetAddress("from@konferenca.test"));
    assertThat(OrganizerMailComposerTest.text(message))
        .contains("Registered at: 2026-10-19T12:00:00.123Z");
  }
}
