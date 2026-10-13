package si.konferenca.registration.service;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Beans of the service layer. */
@Configuration
public class ServiceConfiguration {

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
