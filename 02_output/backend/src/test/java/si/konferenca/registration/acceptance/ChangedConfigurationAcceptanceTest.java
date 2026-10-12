package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.Payloads;
import si.konferenca.registration.acceptance.support.TestInfrastructure;
import tools.jackson.databind.JsonNode;

/** US-003: the backend started with a different options configuration (AC-003-02). */
class ChangedConfigurationAcceptanceTest extends AcceptanceTestBase {

  @DynamicPropertySource
  static void changedConfiguration(DynamicPropertyRegistry registry) {
    TestInfrastructure.register(
        registry,
        Map.of(
            "REGISTRATION_CONFERENCE_CONFIG",
            "classpath:acceptance/conference-acceptance-changed.json"));
  }

  @Test
  void AC_003_02_formConfigurationReflectsAddedAndDeactivatedOptions() {
    HttpResponse<String> response = get("/api/form-config");

    assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
    Map<String, JsonNode> options = FormConfigAcceptanceTest.optionsById(json(response.body()));
    assertThat(options.keySet())
        .containsExactlyInAnyOrder(
            "ws-security", "ws-ai", "ev-opening", "meal-lunch", "other-tour");
    assertThat(options.get("ws-ai").path("name").asString()).isEqualTo("AI in testing workshop");
    assertThat(options.get("ws-ai").path("category").asString()).isEqualTo("WORKSHOP");
  }

  @Test
  void AC_003_02_newConfigurationIsEnforcedOnSubmission() {
    String deactivated = Payloads.uniqueEmail("ac00302a");
    Map<String, Object> body = Payloads.student(deactivated);
    body.put("optionIds", List.of("ws-testing"));
    assertRejectedWithFieldError(postRegistration(body), "optionIds");

    String added = Payloads.uniqueEmail("ac00302b");
    Map<String, Object> withNewOption = Payloads.student(added);
    withNewOption.put("optionIds", List.of("ws-ai", "meal-lunch"));
    registerOk(withNewOption);
    assertThat(registrationsWithEmail(added)).isEqualTo(1);
  }
}
