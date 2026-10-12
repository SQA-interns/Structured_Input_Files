package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.acceptance.support.DefaultContextAcceptanceTest;
import si.konferenca.registration.acceptance.support.Payloads;
import si.konferenca.registration.acceptance.support.TestInfrastructure;
import tools.jackson.databind.JsonNode;

/** US-006 Participant email confirmation (AC-006-01, AC-006-02). */
class ParticipantEmailAcceptanceTest extends DefaultContextAcceptanceTest {

  private static List<String> addresses(JsonNode recipients) {
    return recipients.valueStream().map(r -> r.path("Address").asString()).toList();
  }

  @Test
  void AC_006_01_participantReceivesOneEmailNamingConferenceParticipantAndOptions() {
    String email = Payloads.uniqueEmail("ac00601");

    registerOk(Payloads.external(email));

    List<JsonNode> messages = MAIL.awaitMessagesTo(email, list -> !list.isEmpty());
    assertThat(messages).hasSize(1);
    JsonNode message = MAIL.message(messages.getFirst().path("ID").asString());
    assertThat(addresses(message.path("To"))).containsExactly(email);
    String text = message.path("Text").asString();
    assertThat(message.path("Subject").asString() + "\n" + text)
        .contains(TestInfrastructure.CONFERENCE_NAME);
    assertThat(text).contains("Ana", "Novak", "Secure coding workshop", "Opening reception");
  }

  @Test
  void AC_006_02_lineBreaksHeaderTextAndMarkupCannotInjectHeadersRecipientsOrMarkup() {
    String email = Payloads.uniqueEmail("ac00602");
    String injected = Payloads.uniqueEmail("injected");
    Map<String, Object> body = Payloads.external(email);
    body.put("firstName", "Ana\r\nBcc: " + injected);
    body.put("lastName", "<b>Novak</b><img src=x onerror=alert(1)>\r\nX-Injected: yes");
    body.put("organization", "Acme\nCc: " + injected + "\n\n<h1>Big</h1>");

    registerOk(body);

    List<JsonNode> messages = MAIL.awaitMessagesTo(email, list -> !list.isEmpty());
    assertThat(messages).hasSize(1);
    String id = messages.getFirst().path("ID").asString();
    JsonNode message = MAIL.message(id);
    assertThat(addresses(message.path("To"))).containsExactly(email);
    assertThat(addresses(message.path("Cc"))).isEmpty();
    assertThat(addresses(message.path("Bcc"))).isEmpty();
    assertThat(MAIL.messagesTo(injected)).isEmpty();

    JsonNode headers = MAIL.headers(id);
    assertThat(headers.has("X-Injected")).isFalse();
    assertThat(headers.has("Bcc")).isFalse();
    assertThat(headers.has("Cc")).isFalse();
    assertThat(headers.path("Content-Type").get(0).asString()).startsWith("text/plain");

    assertThat(message.path("HTML").asString()).isEmpty();
    assertThat(message.path("Text").asString())
        .contains("<b>Novak</b><img src=x onerror=alert(1)>");
  }
}
