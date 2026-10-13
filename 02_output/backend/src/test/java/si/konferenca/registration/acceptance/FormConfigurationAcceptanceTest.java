package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** US-003 configurable options: the form configuration served from the configuration file. */
class FormConfigurationAcceptanceTest extends AcceptanceTestBase {

  @Test
  @DisplayName(
      "AC-003-01 form configuration lists only active options with id, name, category and types")
  void ac00301OnlyActiveOptionsListed() {
    Api.Response response = api.getFormConfig();

    assertThat(response.status()).isEqualTo(200);
    Map<String, JsonNode> options = new HashMap<>();
    response.json().get("options").forEach(o -> options.put(o.get("id").asString(), o));
    assertThat(options.keySet())
        .containsExactlyInAnyOrder("ws-a", "ws-ext", "ev-a", "meal-a", "ot-a", "ot-stu");

    assertOption(options.get("ws-a"), "Workshop A", "WORKSHOP", "EXTERNAL", "STUDENT");
    assertOption(options.get("ws-ext"), "Workshop for externals", "WORKSHOP", "EXTERNAL");
    assertOption(options.get("ev-a"), "Evening event", "EVENT", "EXTERNAL", "STUDENT");
    assertOption(options.get("meal-a"), "Lunch", "MEAL", "EXTERNAL", "STUDENT");
    assertOption(options.get("ot-a"), "City tour", "OTHER", "EXTERNAL", "STUDENT");
    assertOption(options.get("ot-stu"), "Student meetup", "OTHER", "STUDENT");
  }

  static void assertOption(JsonNode option, String name, String category, String... types) {
    assertThat(option.get("name").asString()).isEqualTo(name);
    assertThat(option.get("category").asString()).isEqualTo(category);
    List<String> available = new ArrayList<>();
    option.get("availableTo").forEach(t -> available.add(t.asString()));
    assertThat(available).containsExactlyInAnyOrder(types);
  }
}
