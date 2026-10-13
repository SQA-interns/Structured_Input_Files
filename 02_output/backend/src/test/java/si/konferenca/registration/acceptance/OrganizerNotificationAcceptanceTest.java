package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** US-007 organizer notification with the raw registration JSON attached. */
class OrganizerNotificationAcceptanceTest extends AcceptanceTestBase {

  @Test
  @DisplayName("AC-007-01 every organizer address receives a notification with the submitted data")
  void ac00701EveryOrganizerNotified() {
    assertAccepted(api.postRegistration(Submissions.validExternal()));

    for (String organizer : AcceptanceEnvironment.ORGANIZER_EMAILS) {
      List<JsonNode> received = AcceptanceEnvironment.awaitMessagesTo(organizer, 1);
      assertThat(received).as("notification to %s", organizer).hasSize(1);
      String text =
          AcceptanceEnvironment.message(received.get(0).get("ID").asString())
              .get("Text")
              .asString();
      assertThat(text)
          .contains("Ana")
          .contains("Novak")
          .contains("ana.novak@example.si")
          .contains("Institut Primer")
          .contains("Workshop A", "Workshop for externals", "Evening event", "Lunch", "City tour");
    }
  }

  @Test
  @DisplayName("AC-007-02 the notification has a JSON attachment equal to the stored JSON copy")
  void ac00702AttachmentEqualsJsonCopy() throws Exception {
    String id = assertAccepted(api.postRegistration(Submissions.validStudent()));
    JsonNode stored =
        AcceptanceEnvironment.JSON.readTree(Files.readString(AcceptanceEnvironment.jsonCopy(id)));

    for (String organizer : AcceptanceEnvironment.ORGANIZER_EMAILS) {
      List<JsonNode> received = AcceptanceEnvironment.awaitMessagesTo(organizer, 1);
      assertThat(received).as("notification to %s", organizer).hasSize(1);
      String messageId = received.get(0).get("ID").asString();
      JsonNode attachments = AcceptanceEnvironment.message(messageId).get("Attachments");
      JsonNode json = null;
      for (JsonNode a : attachments) {
        if (a.get("ContentType").asString().startsWith("application/json")) {
          json = a;
        }
      }
      assertThat(json).as("JSON attachment to %s", organizer).isNotNull();
      byte[] content = AcceptanceEnvironment.part(messageId, json.get("PartID").asString());
      assertThat(AcceptanceEnvironment.JSON.readTree(new String(content, StandardCharsets.UTF_8)))
          .isEqualTo(stored);
    }
  }
}
