package si.konferenca.registration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/** Entry point of the conference registration API. */
@SpringBootApplication
@EnableConfigurationProperties(RegistrationProperties.class)
public class RegistrationApplication {

  public static void main(String[] args) {
    SpringApplication.run(RegistrationApplication.class, args);
  }
}
