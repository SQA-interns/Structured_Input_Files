package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import tools.jackson.databind.JsonNode;

/** US-004 Registration confirmation: the API data the confirmation is built from (BR-06). */
class RegistrationConfirmationAcceptanceTest extends AcceptanceTestBase {

  @Test
  void ac_004_01_acceptedResponseCarriesTheConfirmationData() {
    String email = uniqueEmail("ac-004-01");

    JsonNode accepted = registerAccepted(validExternal(email));

    assertThat(accepted.path("reference").asString())
        .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    assertThat(accepted.path("submittedAt").asString()).isNotBlank();
    assertThat(accepted.path("type").asString()).isEqualTo("EXTERNAL");
    assertThat(accepted.path("firstName").asString()).isEqualTo("Ana");
    assertThat(accepted.path("lastName").asString()).isEqualTo("Novak");
    assertThat(accepted.path("options").valueStream().map(o -> o.path("name").asString()))
        .containsExactlyInAnyOrder("Delavnica: umetna inteligenca", "Kosilo");
  }

  @Test
  void ac_004_02_rejectedResponseCarriesNoConfirmationAndNoInternals() {
    Map<String, Object> body = validExternal(uniqueEmail("ac-004-02"));
    body.put("email", "not-an-email");

    HttpResponse<String> response = register(body);

    assertThat(response.statusCode()).isEqualTo(400);
    JsonNode error = json(response);
    assertThat(error.has("reference")).isFalse();
    assertThat(error.path("message").asString()).isNotBlank();
    assertNoInternals(response.body());
  }
}
