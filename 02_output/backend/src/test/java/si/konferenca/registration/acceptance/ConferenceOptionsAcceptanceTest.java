package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.TestStack;

/** US-003 Configurable conference options: GET /api/form-config. */
class ConferenceOptionsAcceptanceTest extends AcceptanceTestBase {

  @DynamicPropertySource
  static void settings(DynamicPropertyRegistry registry) {
    TestStack.register(registry);
  }

  @Test
  @DisplayName(
      "AC-003-01 form configuration lists only active options with id, name, category, types")
  void ac_003_01_formConfigurationListsOnlyActiveOptions() throws Exception {
    MvcResult result = mvc.perform(get("/api/form-config")).andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    String json = body(result);
    List<String> ids = JsonPath.read(json, "$.options[*].id");
    assertThat(ids)
        .containsExactlyInAnyOrder(
            "ws-testing", "ws-security", "ev-opening", "meal-lunch", "meal-dinner", "oth-tour");
    assertThat(ids).doesNotContain("ws-retired");

    assertThat(option(json, "ws-testing"))
        .containsEntry("name", "Workshop: Software testing")
        .containsEntry("category", "WORKSHOP");
    assertThat(types(json, "ws-testing")).containsExactlyInAnyOrder("EXTERNAL", "STUDENT");
    assertThat(option(json, "ws-security")).containsEntry("name", "Workshop: Secure coding");
    assertThat(types(json, "ws-security")).containsExactly("EXTERNAL");
    assertThat(option(json, "ev-opening"))
        .containsEntry("name", "Opening reception")
        .containsEntry("category", "EVENT");
    assertThat(option(json, "meal-dinner")).containsEntry("category", "MEAL");
    assertThat(types(json, "meal-dinner")).containsExactly("EXTERNAL");
    assertThat(option(json, "oth-tour")).containsEntry("category", "OTHER");
    assertThat(types(json, "oth-tour")).containsExactlyInAnyOrder("EXTERNAL", "STUDENT");
  }

  static Map<String, Object> option(String json, String id) {
    List<Map<String, Object>> found = JsonPath.read(json, "$.options[?(@.id == '" + id + "')]");
    assertThat(found).as("option %s", id).hasSize(1);
    return found.getFirst();
  }

  static List<String> types(String json, String id) {
    @SuppressWarnings("unchecked")
    List<String> types = (List<String>) option(json, id).get("availableTo");
    return types;
  }
}
