package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * US-003: the backend started with another options file (`acceptance/conference-changed.json`:
 * `ev-new` added, `ws-a` deactivated) serves that configuration, with no code change.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChangedConfigurationAcceptanceTest {

  @LocalServerPort private int port;

  private Api api;

  @DynamicPropertySource
  static void backendProperties(DynamicPropertyRegistry registry) {
    AcceptanceEnvironment.register(
        registry,
        AcceptanceEnvironment.Settings.defaults()
            .withConferenceConfig("classpath:acceptance/conference-changed.json"));
  }

  @BeforeEach
  void resetState() {
    AcceptanceEnvironment.reset();
    api = new Api(port);
  }

  @Test
  @DisplayName("AC-003-02 a different options configuration is reflected without code change")
  void ac00302ChangedConfigurationReflected() {
    Api.Response response = api.getFormConfig();

    assertThat(response.status()).isEqualTo(200);
    List<String> ids = new ArrayList<>();
    response.json().get("options").forEach(o -> ids.add(o.get("id").asString()));
    assertThat(ids)
        .contains("ev-new")
        .doesNotContain("ws-a")
        .containsExactlyInAnyOrder("ws-ext", "ev-a", "ev-new", "meal-a", "ot-a");
  }
}
