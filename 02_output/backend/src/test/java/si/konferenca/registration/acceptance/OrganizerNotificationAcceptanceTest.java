package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import si.konferenca.registration.acceptance.support.AcceptanceEnvironment;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.Submissions;
import tools.jackson.databind.JsonNode;

/** US-007: organizer notification with the JSON copy attached (email.md). */
class OrganizerNotificationAcceptanceTest extends AcceptanceTestBase {

  @Test
  void AC_007_01_eachOrganizerAddressReceivesTheSubmittedRegistrationData() throws Exception {
    Map<String, Object> body = Submissions.validExternal();

    MvcResult result = submit(body);

    assertThat(result.getResponse().getStatus()).isEqualTo(201);
    String id = json(result).path("id").asString();
    for (String organizer :
        List.of(AcceptanceEnvironment.ORGANIZER_ONE, AcceptanceEnvironment.ORGANIZER_TWO)) {
      JsonNode message = awaitNotification(organizer, id);
      assertThat(message).as("notification to " + organizer).isNotNull();
      assertThat(message.path("Text").asString())
          .contains(id)
          .contains("EXTERNAL")
          .contains("Ana")
          .contains("Novak")
          .contains((String) body.get("email"))
          .contains("Institut Jozef Stefan")
          .contains("ws-ai", "ev-dinner", "meal-lunch", "oth-tour")
          .contains("privacy");
    }
  }

  @Test
  void AC_007_02_notificationHasJsonAttachmentEqualToTheStoredCopy() throws Exception {
    MvcResult result = submit(Submissions.validStudent());

    assertThat(result.getResponse().getStatus()).isEqualTo(201);
    String id = json(result).path("id").asString();
    JsonNode message = awaitNotification(AcceptanceEnvironment.ORGANIZER_ONE, id);
    assertThat(message).isNotNull();
    JsonNode attachment = null;
    for (JsonNode a : message.path("Attachments")) {
      if (("registration-" + id + ".json").equals(a.path("FileName").asString())) {
        attachment = a;
      }
    }
    assertThat(attachment).as("attachment registration-<id>.json").isNotNull();
    assertThat(attachment.path("ContentType").asString()).startsWith("application/json");
    byte[] attached =
        mailpit.part(message.path("ID").asString(), attachment.path("PartID").asString());
    byte[] stored = Files.readAllBytes(jsonCopyDir().resolve(id + ".json"));
    assertThat(attached).isEqualTo(stored);
  }

  /** The organizer message for the registration id, waiting at most 15 seconds. */
  private JsonNode awaitNotification(String organizer, String id) {
    long deadline = System.nanoTime() + 15_000_000_000L;
    do {
      for (JsonNode message : mailpit.messagesTo(organizer)) {
        if (message.path("Text").asString().contains(id)) {
          return message;
        }
      }
      sleep();
    } while (System.nanoTime() < deadline);
    return null;
  }

  private static void sleep() {
    try {
      Thread.sleep(250);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
