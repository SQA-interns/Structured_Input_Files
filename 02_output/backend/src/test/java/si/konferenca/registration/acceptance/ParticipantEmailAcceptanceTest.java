package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** US-006 participant email confirmation, caught by Mailpit. */
class ParticipantEmailAcceptanceTest extends AcceptanceTestBase {

  private static final String PARTICIPANT = "ana.novak@example.si";

  @Test
  @DisplayName(
      "AC-006-01 one email to the participant names the conference, the participant and the options")
  void ac00601ConfirmationEmailSent() throws InterruptedException {
    assertAccepted(api.postRegistration(Submissions.validExternal()));

    List<JsonNode> received = AcceptanceEnvironment.awaitMessagesTo(PARTICIPANT, 1);
    Thread.sleep(1000);
    assertThat(AcceptanceEnvironment.messagesTo(PARTICIPANT))
        .as("emails to participant")
        .hasSize(1);

    JsonNode message = AcceptanceEnvironment.message(received.get(0).get("ID").asString());
    String content = message.get("Subject").asString() + "\n" + message.get("Text").asString();
    assertThat(content)
        .contains(AcceptanceEnvironment.CONFERENCE_NAME)
        .contains("Ana")
        .contains("Novak")
        .contains("Workshop A", "Workshop for externals", "Evening event", "Lunch", "City tour");
  }

  @Test
  @DisplayName(
      "AC-006-02 line breaks, header-like text and markup add no headers or recipients and stay literal")
  void ac00602NoHeaderOrMarkupInjection() throws InterruptedException {
    ObjectNode body = Submissions.validExternal();
    body.put("firstName", "Ana\r\nBcc: attacker@evil.example");
    body.put("lastName", "<b>Novak</b>");
    body.put("organization", "Org\nX-Injected: yes <script>alert(1)</script>");

    assertAccepted(api.postRegistration(body));

    List<JsonNode> received = AcceptanceEnvironment.awaitMessagesTo(PARTICIPANT, 1);
    assertThat(received).as("confirmation email").hasSize(1);
    Thread.sleep(1000);
    assertThat(AcceptanceEnvironment.messagesTo("attacker@evil.example")).isEmpty();
    assertThat(AcceptanceEnvironment.messages())
        .as("participant email plus one per organizer address, nothing else")
        .hasSize(1 + AcceptanceEnvironment.ORGANIZER_EMAILS.size());

    String id = received.get(0).get("ID").asString();
    JsonNode message = AcceptanceEnvironment.message(id);
    List<String> to = new ArrayList<>();
    message.get("To").forEach(t -> to.add(t.get("Address").asString()));
    assertThat(to).containsExactly(PARTICIPANT);
    assertThat(message.get("Cc")).isEmpty();
    assertThat(message.get("Bcc")).isEmpty();

    List<String> headerNames = new ArrayList<>();
    AcceptanceEnvironment.headers(id).propertyNames().forEach(headerNames::add);
    assertThat(headerNames)
        .noneMatch(h -> h.equalsIgnoreCase("Bcc"))
        .noneMatch(h -> h.equalsIgnoreCase("X-Injected"));

    assertThat(message.get("HTML").asString()).as("no HTML part").isEmpty();
    assertThat(message.get("Text").asString())
        .contains("<b>Novak</b>")
        .contains("<script>alert(1)</script>");
  }
}
