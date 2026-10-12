package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.acceptance.support.DefaultContextAcceptanceTest;
import si.konferenca.registration.acceptance.support.Payloads;
import si.konferenca.registration.acceptance.support.TestInfrastructure;
import tools.jackson.databind.JsonNode;

/** US-007 Organizer notification (AC-007-01, AC-007-02). */
class OrganizerNotificationAcceptanceTest extends DefaultContextAcceptanceTest {

  @Test
  void AC_007_01_eachOrganizerAddressReceivesTheSubmittedData() {
    String email = Payloads.uniqueEmail("ac00701");
    Map<String, Object> body = Payloads.student(email);

    String id = registerOk(body);

    for (String organizer : TestInfrastructure.ORGANIZER_EMAILS) {
      List<JsonNode> messages = MAIL.awaitMessagesToMentioning(organizer, email);
      assertThat(messages).as("notification to %s", organizer).hasSize(1);
      JsonNode message = MAIL.message(messages.getFirst().path("ID").asString());
      assertThat(message.path("To").valueStream().map(r -> r.path("Address").asString()))
          .contains(organizer)
          .doesNotContain(email);
      assertThat(message.path("Text").asString())
          .contains(
              id,
              "STUDENT",
              "Bor",
              "Kranjc",
              email,
              "University of Maribor",
              "Computer Science",
              "63210001",
              "Test automation workshop",
              "Lunch",
              "privacy");
    }
  }

  @Test
  void AC_007_02_notificationHasJsonAttachmentEqualToTheStoredCopy() throws Exception {
    String email = Payloads.uniqueEmail("ac00702");

    String id = registerOk(Payloads.external(email));

    byte[] storedCopy = Files.readAllBytes(copyFile(id));
    for (String organizer : TestInfrastructure.ORGANIZER_EMAILS) {
      List<JsonNode> messages = MAIL.awaitMessagesToMentioning(organizer, email);
      assertThat(messages).as("notification to %s", organizer).hasSize(1);
      String messageId = messages.getFirst().path("ID").asString();
      JsonNode attachments = MAIL.message(messageId).path("Attachments");
      assertThat(attachments.size()).isEqualTo(1);
      JsonNode attachment = attachments.get(0);
      assertThat(attachment.path("FileName").asString()).isEqualTo("registration-" + id + ".json");
      assertThat(attachment.path("ContentType").asString()).startsWith("application/json");
      byte[] content = MAIL.part(messageId, attachment.path("PartID").asString());
      assertThat(content).isEqualTo(storedCopy);
    }
  }
}
