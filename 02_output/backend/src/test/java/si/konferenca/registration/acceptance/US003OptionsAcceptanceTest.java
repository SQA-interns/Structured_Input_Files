package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** US-003 configurable options, through {@code GET /api/options}. */
class US003OptionsAcceptanceTest extends AcceptanceTestBase {

  @Test
  @DisplayName("AC-003-01 only active options are returned, with id, name, category and types")
  void ac003_01_onlyActiveOptionsAreReturned() throws Exception {
    Response response = api.get("/api/options");

    assertThat(response.status()).as(response.toString()).isEqualTo(200);
    List<Map<String, Object>> options = response.json("$");
    assertThat(options)
        .extracting(o -> o.get("id"))
        .containsExactly("ws-a", "ws-b", "ws-ext", "ev-dinner", "meal-lunch", "other-fair");
    assertThat(options.get(0))
        .containsEntry("name", "Workshop A: Testing")
        .containsEntry("category", "WORKSHOP")
        .containsEntry("registrationTypes", List.of("EXTERNAL", "STUDENT"));
    assertThat(options.get(2)).containsEntry("registrationTypes", List.of("EXTERNAL"));
    assertThat(options.get(3)).containsEntry("category", "EVENT");
    assertThat(options.get(4)).containsEntry("category", "MEAL");
    assertThat(options.get(5))
        .containsEntry("category", "OTHER")
        .containsEntry("registrationTypes", List.of("STUDENT"));
  }
}
