package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.acceptance.support.DefaultContextAcceptanceTest;
import tools.jackson.databind.JsonNode;

/** US-003 Configurable conference options (AC-003-01). */
class FormConfigAcceptanceTest extends DefaultContextAcceptanceTest {

  static Map<String, JsonNode> optionsById(JsonNode config) {
    Map<String, JsonNode> byId = new HashMap<>();
    config.path("options").forEach(option -> byId.put(option.path("id").asString(), option));
    return byId;
  }

  static List<String> types(JsonNode option) {
    return option.path("availableTo").valueStream().map(JsonNode::asString).sorted().toList();
  }

  @Test
  void AC_003_01_formConfigurationListsOnlyActiveOptionsWithTheirAttributes() {
    HttpResponse<String> response = get("/api/form-config");

    assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
    Map<String, JsonNode> options = optionsById(json(response.body()));
    assertThat(options.keySet())
        .containsExactlyInAnyOrder(
            "ws-testing", "ws-security", "ev-opening", "meal-lunch", "meal-vegan", "other-tour");
    assertThat(options).doesNotContainKey("ws-legacy");

    JsonNode testing = options.get("ws-testing");
    assertThat(testing.path("name").asString()).isEqualTo("Test automation workshop");
    assertThat(testing.path("category").asString()).isEqualTo("WORKSHOP");
    assertThat(types(testing)).containsExactly("EXTERNAL", "STUDENT");

    JsonNode security = options.get("ws-security");
    assertThat(security.path("name").asString()).isEqualTo("Secure coding workshop");
    assertThat(types(security)).containsExactly("EXTERNAL");

    assertThat(options.get("ev-opening").path("category").asString()).isEqualTo("EVENT");
    assertThat(options.get("meal-lunch").path("category").asString()).isEqualTo("MEAL");
    assertThat(options.get("other-tour").path("category").asString()).isEqualTo("OTHER");
    assertThat(options.get("other-tour").path("name").asString()).isEqualTo("City tour");
  }

  @Test
  void AC_003_01_formConfigurationListsTheConsentsUnticked() {
    HttpResponse<String> response = get("/api/form-config");
    assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
    JsonNode config = json(response.body());

    JsonNode privacy =
        config
            .path("consents")
            .valueStream()
            .filter(c -> "privacy".equals(c.path("id").asString()))
            .findFirst()
            .orElseThrow();
    assertThat(privacy.path("mandatory").asBoolean()).isTrue();
    assertThat(privacy.path("text").asString())
        .isEqualTo(
            "I agree that my personal data is processed for organizing the conference and that it"
                + " is shared with the conference organizers.");
    assertThat(config.path("recaptchaTestMode").asBoolean()).isTrue();
  }
}
