package si.konferenca.registration.infrastructure.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import si.konferenca.registration.application.RegistrationNotifier;
import si.konferenca.registration.domain.ConferenceConfiguration;

/** Email wiring (email.md); sending runs on its own small pool, off the request thread. */
@Configuration
public class MailBeans {

  @Bean
  ThreadPoolTaskExecutor mailExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(2);
    executor.setThreadNamePrefix("mail-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    return executor;
  }

  @Bean
  ParticipantMailComposer participantMailComposer(
      JavaMailSender sender,
      @Value("${app.mail.from}") String from,
      ConferenceConfiguration configuration) {
    return new ParticipantMailComposer(sender, from, configuration.conferenceName());
  }

  @Bean
  RegistrationNotifier registrationNotifier(
      JavaMailSender sender,
      ParticipantMailComposer participantMail,
      ThreadPoolTaskExecutor mailExecutor) {
    return new MailRegistrationNotifier(sender, participantMail, null, mailExecutor);
  }
}
