package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.TestStack;

/** AC-003-02: the backend started with another options file offers the changed options. */
class ConferenceOptionsReconfigurationAcceptanceTest extends AcceptanceTestBase {

  @DynamicPropertySource
  static void settings(DynamicPropertyRegistry registry) {
    TestStack.register(
        registry,
        Map.of("CONFERENCE_CONFIG", "classpath:acceptance/conference-acceptance-changed.json"));
  }

  @Test
  @DisplayName("AC-003-02 form configuration reflects an added and a deactivated option")
  void ac_003_02_changedConfigurationIsReflected() throws Exception {
    MvcResult result = mvc.perform(get("/api/form-config")).andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    String json = body(result);
    List<String> ids = JsonPath.read(json, "$.options[*].id");
    assertThat(ids)
        .containsExactlyInAnyOrder(
            "ws-security", "ev-opening", "meal-lunch", "oth-tour", "oth-hike")
        .doesNotContain("ws-testing");
    Map<String, Object> hike = ConferenceOptionsAcceptanceTest.option(json, "oth-hike");
    assertThat(hike).containsEntry("name", "Mountain hike").containsEntry("category", "OTHER");
    assertThat(ConferenceOptionsAcceptanceTest.types(json, "oth-hike")).containsExactly("STUDENT");
  }

  @Test
  @DisplayName("AC-003-02 the deactivated option is rejected and the added option is accepted")
  void ac_003_02_changedConfigurationAppliesToRegistrations() throws Exception {
    Map<String, Object> deactivated = validStudent();
    deactivated.put("optionIds", new ArrayList<>(List.of("ws-testing")));
    assertRejectedWithFieldError(submit(deactivated), "optionIds");

    Map<String, Object> added = validStudent();
    added.put("optionIds", new ArrayList<>(List.of("oth-hike")));
    register(added);
  }
}
