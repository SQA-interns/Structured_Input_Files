package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import si.konferenca.registration.acceptance.support.AcceptanceEnvironment;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.MailpitClient;
import si.konferenca.registration.acceptance.support.Submissions;
import tools.jackson.databind.JsonNode;

/** US-006: participant confirmation email (email.md), read from Mailpit. */
class ParticipantEmailAcceptanceTest extends AcceptanceTestBase {

  private static final String INTRUDER = "intruder@evil.example";

  @Test
  void AC_006_01_oneConfirmationEmailNamesConferenceParticipantAndOptions() throws Exception {
    Map<String, Object> body = Submissions.validExternal();
    String email = (String) body.get("email");

    MvcResult result = submit(body);

    assertThat(result.getResponse().getStatus()).isEqualTo(201);
    List<JsonNode> messages = mailpit.awaitMessagesTo(email, 1);
    assertThat(messages).hasSize(1);
    assertThat(mailpit.messagesToAfterGracePeriod(email)).hasSize(1);
    String text = messages.get(0).path("Text").asString();
    assertThat(text)
        .contains(AcceptanceEnvironment.CONFERENCE_NAME)
        .contains("Ana")
        .contains("Novak")
        .contains("Workshop: AI in practice", "Conference dinner", "Lunch", "City tour");
  }

  @Test
  void AC_006_02_lineBreaksHeaderTextAndMarkupCannotInjectHeadersOrMarkup() throws Exception {
    Map<String, Object> body = Submissions.validExternal();
    String email = (String) body.get("email");
    body.put("firstName", "Ana\r\nBcc: " + INTRUDER + "\r\nSubject: hacked");
    body.put("lastName", "<b>Novak</b><script>alert(1)</script>");
    body.put("organization", "IJS\nCc: " + INTRUDER);

    MvcResult result = submit(body);

    assertThat(result.getResponse().getStatus()).isEqualTo(201);
    List<JsonNode> messages = mailpit.awaitMessagesTo(email, 1);
    assertThat(messages).hasSize(1);
    JsonNode message = messages.get(0);
    assertThat(MailpitClient.recipients(message, "To")).containsExactly(email.toLowerCase());
    assertThat(MailpitClient.recipients(message, "Cc")).isEmpty();
    assertThat(MailpitClient.recipients(message, "Bcc")).isEmpty();
    assertThat(message.path("Subject").asString()).doesNotContain("hacked");
    assertThat(message.path("HTML").asString()).isEmpty();
    assertThat(message.path("Text").asString()).contains("<b>Novak</b><script>alert(1)</script>");

    JsonNode headers = mailpit.headers(message.path("ID").asString());
    List<String> headerNames = new ArrayList<>();
    headers.propertyNames().forEach(name -> headerNames.add(name.toLowerCase()));
    assertThat(headerNames).doesNotContain("bcc", "cc");
    assertThat(headers.toString()).doesNotContain(INTRUDER).doesNotContain("hacked");

    assertThat(mailpit.messagesToAfterGracePeriod(INTRUDER)).isEmpty();
    for (JsonNode any : mailpit.allMessages()) {
      assertThat(MailpitClient.recipients(any, "Cc")).doesNotContain(INTRUDER);
      assertThat(MailpitClient.recipients(any, "Bcc")).doesNotContain(INTRUDER);
    }
  }
}
