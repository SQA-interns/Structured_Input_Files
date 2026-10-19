package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import tools.jackson.databind.JsonNode;

/** US-003: GET /api/form-config lists the active configured options (conference-test.json). */
class FormConfigAcceptanceTest extends AcceptanceTestBase {

  @Test
  void AC_003_01_formConfigurationListsOnlyActiveOptionsWithTheirAttributes() throws Exception {
    MvcResult result = mvc.perform(get("/api/form-config")).andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    JsonNode config =
        JSON.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    Map<String, JsonNode> options = new HashMap<>();
    for (JsonNode option : config.path("options")) {
      options.put(option.path("id").asString(), option);
    }

    assertThat(options.keySet())
        .containsExactlyInAnyOrder("ws-ai", "ev-dinner", "meal-lunch", "oth-tour", "oth-mentoring");
    assertOption(
        options.get("ws-ai"), "Workshop: AI in practice", "WORKSHOP", "EXTERNAL", "STUDENT");
    assertOption(options.get("ev-dinner"), "Conference dinner", "EVENT", "EXTERNAL", "STUDENT");
    assertOption(options.get("meal-lunch"), "Lunch", "MEAL", "EXTERNAL", "STUDENT");
    assertOption(options.get("oth-tour"), "City tour", "OTHER", "EXTERNAL");
    assertOption(options.get("oth-mentoring"), "Student mentoring", "OTHER", "STUDENT");
  }

  static void assertOption(JsonNode option, String name, String category, String... types) {
    assertThat(option.path("name").asString()).isEqualTo(name);
    assertThat(option.path("category").asString()).isEqualTo(category);
    Set<String> availableTo = new HashSet<>();
    option.path("availableTo").forEach(t -> availableTo.add(t.asString()));
    assertThat(availableTo).containsExactlyInAnyOrder(types);
  }
}
