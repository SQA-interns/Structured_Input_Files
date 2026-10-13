package si.konferenca.registration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class RegistrationPropertiesTest {

  @Test
  void recipientsAndOriginsAreStrippedWithoutBlanks() {
    assertThat(
            new RegistrationProperties.Mail("f", Arrays.asList(" a@b.co ", "", "  ", "c@d.co"))
                .organizerRecipients())
        .containsExactly("a@b.co", "c@d.co");
    assertThat(new RegistrationProperties.Mail("f", null).organizerRecipients()).isEmpty();
    assertThat(new RegistrationProperties.Cors(List.of(" http://x ", " ")).allowedOrigins())
        .containsExactly("http://x");
    assertThat(new RegistrationProperties.Cors(null).allowedOrigins()).isEmpty();
  }

  @Test
  void secretsAreNotPrinted() {
    assertThat(new RegistrationProperties.Organizer("boss", "pw-123456", true).toString())
        .isEqualTo("Organizer[httpsOnly=true]");
    assertThat(
            new RegistrationProperties.Recaptcha(
                    false, "site", "secret-123456", URI.create("http://v"))
                .toString())
        .isEqualTo("Recaptcha[testMode=false, verifyUrl=http://v]");
  }
}
