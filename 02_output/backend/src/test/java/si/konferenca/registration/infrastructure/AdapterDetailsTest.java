package si.konferenca.registration.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import si.konferenca.registration.domain.InvalidConfigurationException;
import si.konferenca.registration.infrastructure.captcha.RecaptchaVerifier;
import si.konferenca.registration.infrastructure.config.ConferenceConfigurationLoader;

/** Covers AC-003-03 and SR-01 details of the adapters (phase 5 survivors). */
class AdapterDetailsTest {

  @TempDir Path dir;

  @Test
  void AC_003_03_unknownPropertiesInsideConsentsOrOptionsAreConfigurationErrors() throws Exception {
    Path consent = dir.resolve("consent.json");
    Files.writeString(
        consent,
        "{\"conferenceName\":\"C\",\"consents\":[{\"id\":\"p\",\"text\":\"T\",\"mandatory\":true,"
            + "\"preselected\":true}],\"options\":[]}",
        StandardCharsets.UTF_8);
    Path option = dir.resolve("option.json");
    Files.writeString(
        option,
        "{\"conferenceName\":\"C\",\"consents\":[],\"options\":[{\"id\":\"x\",\"name\":\"X\","
            + "\"category\":\"MEAL\",\"active\":true,\"price\":10}]}",
        StandardCharsets.UTF_8);

    ConferenceConfigurationLoader loader = new ConferenceConfigurationLoader();
    assertThatThrownBy(() -> loader.load(consent))
        .isInstanceOf(InvalidConfigurationException.class)
        .hasMessageContaining("preselected");
    assertThatThrownBy(() -> loader.load(option))
        .isInstanceOf(InvalidConfigurationException.class)
        .hasMessageContaining("price");
  }

  @Test
  void SR_01_anInterruptedVerificationRejectsAndKeepsTheInterrupt() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/v",
        exchange -> {
          try {
            Thread.sleep(3000);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          exchange.close();
        });
    server.start();
    try {
      RecaptchaVerifier verifier =
          new RecaptchaVerifier(
              URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v"),
              "s",
              Duration.ofSeconds(10));
      Thread.currentThread().interrupt();

      boolean accepted = verifier.verify("token");

      assertThat(accepted).isFalse();
      assertThat(Thread.interrupted()).isTrue();
    } finally {
      Thread.interrupted();
      server.stop(0);
    }
  }
}
