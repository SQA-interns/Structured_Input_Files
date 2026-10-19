package si.konferenca.registration.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import si.konferenca.registration.domain.ConferenceConfiguration;
import si.konferenca.registration.domain.ConsentDefinition;

/** Covers AC-006-01, AC-007-01: the mail bean factories, called directly (phase 5 survivors). */
class MailBeansTest {

  final MailBeans beans = new MailBeans();
  final JavaMailSenderImpl sender = new JavaMailSenderImpl();
  final ConferenceConfiguration config =
      ConferenceConfiguration.of(
          "Konferenca", List.of(new ConsentDefinition("privacy", "I agree.", true)), List.of());

  @Test
  void AC_007_01_organizerAddressesAreSplitTrimmedAndBlankOnesSkipped() throws Exception {
    OrganizerMailComposer composer =
        beans.organizerMailComposer(sender, "from@k.si", " one@k.si, ,two@k.si ", config);

    var messages =
        composer.compose(
            ParticipantMailComposerTest.registration(
                "Ana", "Novak", ParticipantMailComposerTest.OPTIONS));
    assertThat(messages).hasSize(2);
    assertThat(messages.get(0).getRecipients(Message.RecipientType.TO))
        .containsExactly(new InternetAddress("one@k.si"));
    assertThat(messages.get(1).getRecipients(Message.RecipientType.TO))
        .containsExactly(new InternetAddress("two@k.si"));
  }

  @Test
  void AC_007_01_noOrganizerAddressStopsTheStart() {
    assertThatThrownBy(() -> beans.organizerMailComposer(sender, "from@k.si", " , ", config))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void AC_006_01_participantComposerAndNotifierAreCreated() throws Exception {
    ParticipantMailComposer participant =
        beans.participantMailComposer(sender, "from@k.si", config);

    assertThat(
            participant
                .compose(
                    ParticipantMailComposerTest.registration(
                        "Ana", "Novak", ParticipantMailComposerTest.OPTIONS))
                .getSubject())
        .isEqualTo("Registration confirmation - Konferenca");
    assertThat(
            beans.registrationNotifier(
                sender,
                participant,
                beans.organizerMailComposer(sender, "from@k.si", "o@k.si", config),
                beans.mailExecutor()))
        .isInstanceOf(MailRegistrationNotifier.class);
  }

  @Test
  void AC_006_01_mailExecutorIsASmallNamedPoolThatFinishesPendingMailOnShutdown() throws Exception {
    ThreadPoolTaskExecutor executor = beans.mailExecutor();
    executor.initialize();
    AtomicBoolean finished = new AtomicBoolean(false);
    AtomicBoolean threadNamed = new AtomicBoolean(false);

    executor.execute(
        () -> {
          threadNamed.set(Thread.currentThread().getName().startsWith("mail-"));
          try {
            Thread.sleep(300);
            finished.set(true);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        });
    Thread.sleep(50);
    executor.shutdown();
    executor.getThreadPoolExecutor().awaitTermination(5, TimeUnit.SECONDS);

    assertThat(executor.getCorePoolSize()).isEqualTo(2);
    assertThat(executor.getMaxPoolSize()).isEqualTo(2);
    assertThat(threadNamed).isTrue();
    assertThat(finished).isTrue();
  }
}
