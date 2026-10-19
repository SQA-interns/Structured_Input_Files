package si.konferenca.registration.infrastructure.mail;

import java.util.Arrays;
import java.util.List;
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
  OrganizerMailComposer organizerMailComposer(
      JavaMailSender sender,
      @Value("${app.mail.from}") String from,
      @Value("${app.organizer.emails}") String organizerEmails,
      ConferenceConfiguration configuration) {
    List<String> organizers =
        Arrays.stream(organizerEmails.split(","))
            .map(String::strip)
            .filter(address -> !address.isEmpty())
            .toList();
    if (organizers.isEmpty()) {
      throw new IllegalStateException("ORGANIZER_EMAILS is empty");
    }
    return new OrganizerMailComposer(sender, from, configuration.conferenceName(), organizers);
  }

  @Bean
  RegistrationNotifier registrationNotifier(
      JavaMailSender sender,
      ParticipantMailComposer participantMail,
      OrganizerMailComposer organizerMail,
      ThreadPoolTaskExecutor mailExecutor) {
    return new MailRegistrationNotifier(sender, participantMail, organizerMail, mailExecutor);
  }
}
