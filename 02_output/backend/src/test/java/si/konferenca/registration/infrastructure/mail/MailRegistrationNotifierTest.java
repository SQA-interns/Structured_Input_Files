package si.konferenca.registration.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import si.konferenca.registration.domain.Registration;

class MailRegistrationNotifierTest {

  final List<MimeMessage> sent = new ArrayList<>();
  final List<Runnable> queued = new ArrayList<>();
  boolean failSending = false;

  final JavaMailSenderImpl sender =
      new JavaMailSenderImpl() {
        @Override
        public void send(MimeMessage message) {
          if (failSending) {
            throw new MailSendException(
                "Connection refused: smtp.example.com:25 for ana@example.com");
          }
          sent.add(message);
        }
      };

  final Executor executor = queued::add;
  final ParticipantMailComposer composer =
      new ParticipantMailComposer(sender, "registration@konferenca.test", "Konferenca 2026");
  final MailRegistrationNotifier notifier =
      new MailRegistrationNotifier(sender, composer, executor);

  final ListAppender<ILoggingEvent> logs = new ListAppender<>();
  final Logger logger = (Logger) LoggerFactory.getLogger(MailRegistrationNotifier.class);

  final Registration registration =
      ParticipantMailComposerTest.registration("Ana", "Novak", ParticipantMailComposerTest.OPTIONS);

  @BeforeEach
  void attachAppender() {
    logs.start();
    logger.addAppender(logs);
  }

  @AfterEach
  void detachAppender() {
    logger.detachAppender(logs);
  }

  @Test
  void AC_006_01_confirmationIsSentOnTheExecutor() {
    notifier.registrationAccepted(registration);

    assertThat(sent).isEmpty();
    assertThat(queued).hasSize(1);
    queued.get(0).run();
    assertThat(sent).hasSize(1);
  }

  @Test
  void AC_006_03_sendFailureIsLoggedWithTheRegistrationIdOnly() {
    failSending = true;

    notifier.registrationAccepted(registration);
    assertThatCode(() -> queued.forEach(Runnable::run)).doesNotThrowAnyException();

    assertThat(logs.list).isNotEmpty();
    String logged =
        logs.list.stream().map(ILoggingEvent::getFormattedMessage).reduce("", String::concat);
    assertThat(logged).contains(registration.id().toString());
    assertThat(logged).doesNotContain("ana@example.com", "Ana", "Novak", "smtp.example.com");
    assertThat(logs.list).allSatisfy(e -> assertThat(e.getThrowableProxy()).isNull());
  }
}
