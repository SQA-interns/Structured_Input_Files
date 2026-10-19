package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import si.konferenca.registration.acceptance.support.AcceptanceEnvironment;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * US-003: the same backend build started with conference-changed.json (ws-security added, ws-ai
 * deactivated) serves the new options.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ChangedConfigurationAcceptanceTest {

  @Autowired MockMvc mvc;

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    AcceptanceEnvironment.register(
        registry,
        Map.of(
            "app.conference.config-file",
            AcceptanceEnvironment.resource("conference-changed.json").toString(),
            "app.json-copy.dir",
            AcceptanceEnvironment.newTempDir("json-copies-changed").toString()));
  }

  @Test
  void AC_003_02_formConfigurationReflectsADifferentOptionsConfiguration() throws Exception {
    MvcResult result = mvc.perform(get("/api/form-config")).andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    JsonNode config =
        JsonMapper.builder()
            .build()
            .readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    List<String> ids = new ArrayList<>();
    config.path("options").forEach(option -> ids.add(option.path("id").asString()));
    assertThat(ids)
        .containsExactlyInAnyOrder("ev-dinner", "meal-lunch", "oth-tour", "ws-security")
        .doesNotContain("ws-ai", "oth-mentoring");
  }
}
