package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.TestEnvironment;
import tools.jackson.databind.JsonNode;

/** US-006 Participant email confirmation (and SR-05 for both emails). */
class ParticipantEmailAcceptanceTest extends AcceptanceTestBase {

  /** Headers set by the transport for the recipient, not by the application. */
  private static final Set<String> TRANSPORT_HEADERS =
      Set.of("to", "received", "return-path", "delivered-to", "x-original-to");

  @Test
  void ac_006_01_participantReceivesPlainTextConfirmation() {
    String email = uniqueEmail("ac-006-01");

    JsonNode accepted = registerAccepted(validExternal(email));
    String reference = accepted.path("reference").asString();

    JsonNode message = MAILPIT.awaitSingleMessageTo(email, m -> true);
    assertThat(message.path("From").path("Address").asString()).isEqualTo(TestEnvironment.SENDER);
    assertThat(message.path("Subject").asString())
        .contains(TestEnvironment.CONFERENCE_NAME)
        .contains(reference);
    assertThat(message.path("Text").asString())
        .contains("Ana", "Novak", reference, "External participant")
        .contains("Delavnica: umetna inteligenca", "Kosilo");
    assertThat(message.path("HTML").asString()).isEmpty();
    assertThat(MAILPIT.messagesTo(email)).hasSize(1);
  }

  @Test
  void ac_006_01_studentConfirmationNamesTheStudentType() {
    String email = uniqueEmail("ac-006-01-student");

    registerAccepted(validStudent(email));

    JsonNode message = MAILPIT.awaitSingleMessageTo(email, m -> true);
    assertThat(message.path("Text").asString()).contains("Student", "Študentski večer");
  }

  @Test
  void ac_006_02_markupStaysLiteralAndHeadersCarryNoParticipantInput() {
    String email = uniqueEmail("ac-006-02");
    Map<String, Object> body = validExternal(email);
    body.put("firstName", "<b>Ana</b>");
    body.put("lastName", "<script>alert(1)</script>");

    JsonNode accepted = registerAccepted(body);
    String reference = accepted.path("reference").asString();

    JsonNode participantMail = MAILPIT.awaitSingleMessageTo(email, m -> true);
    assertThat(participantMail.path("Text").asString())
        .contains("<b>Ana</b>", "<script>alert(1)</script>");
    assertThat(participantMail.path("HTML").asString()).isEmpty();
    assertHeadersFreeOfInput(participantMail, email);

    JsonNode organizerMail =
        MAILPIT.awaitSingleMessageTo(
            TestEnvironment.ORGANIZER_ONE, m -> m.path("Subject").asString().contains(reference));
    assertThat(organizerMail.path("Text").asString())
        .contains("<b>Ana</b>", "<script>alert(1)</script>");
    assertThat(organizerMail.path("HTML").asString()).isEmpty();
    assertHeadersFreeOfInput(organizerMail, null);
  }

  /** No header except To (the participant address, when given) contains participant input. */
  private void assertHeadersFreeOfInput(JsonNode message, String participantAddress) {
    JsonNode headers = MAILPIT.headers(message.path("ID").asString());
    Iterator<Map.Entry<String, JsonNode>> fields = headers.properties().iterator();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> header = fields.next();
      String values = header.getValue().toString();
      assertThat(values).as(header.getKey()).doesNotContain("Ana</b>").doesNotContain("script");
      if (participantAddress != null
          && !TRANSPORT_HEADERS.contains(header.getKey().toLowerCase(Locale.ROOT))) {
        assertThat(values).as(header.getKey()).doesNotContain(participantAddress);
      }
    }
  }
}
