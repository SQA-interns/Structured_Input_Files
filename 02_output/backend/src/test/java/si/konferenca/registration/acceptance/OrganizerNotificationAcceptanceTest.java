package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.TestEnvironment;
import tools.jackson.databind.JsonNode;

/** US-007 Organizer notification (and SR-07 for the organizer email). */
class OrganizerNotificationAcceptanceTest extends AcceptanceTestBase {

  @Test
  void ac_007_01_everyOrganizerReceivesTheDataWithTheJsonCopyAttached() throws Exception {
    String email = uniqueEmail("ac-007-01");

    JsonNode accepted = registerAccepted(validExternal(email));
    String reference = accepted.path("reference").asString();

    byte[] copy = Files.readAllBytes(jsonCopy(TestEnvironment.JSON_DIR, reference));
    for (String organizer : List.of(TestEnvironment.ORGANIZER_ONE, TestEnvironment.ORGANIZER_TWO)) {
      List<JsonNode> found =
          MAILPIT.awaitMessagesTo(
              organizer,
              m -> m.path("Subject").asString().contains(reference),
              1,
              Duration.ofSeconds(20));
      assertThat(found).as(organizer).hasSize(1);
      JsonNode message = MAILPIT.message(found.get(0).path("ID").asString());
      assertThat(message.path("From").path("Address").asString()).isEqualTo(TestEnvironment.SENDER);
      assertThat(message.path("Text").asString())
          .contains(reference, "Ana", "Novak", email, "Institut Jožef Stefan")
          .contains("Delavnica: umetna inteligenca", "Kosilo", "data-processing");
      JsonNode attachments = message.path("Attachments");
      assertThat(attachments.size()).isEqualTo(1);
      JsonNode attachment = attachments.get(0);
      assertThat(attachment.path("FileName").asString())
          .isEqualTo("registration-" + reference + ".json");
      assertThat(attachment.path("ContentType").asString()).startsWith("application/json");
      byte[] attached =
          MAILPIT.part(message.path("ID").asString(), attachment.path("PartID").asString());
      assertThat(attached).isEqualTo(copy);
    }
  }

  @Test
  void ac_007_02_organizerEmailContainsOnlyRegistrationData() {
    String email = uniqueEmail("ac-007-02");

    JsonNode accepted = registerAccepted(validStudent(email));
    String reference = accepted.path("reference").asString();

    JsonNode message =
        MAILPIT.awaitSingleMessageTo(
            TestEnvironment.ORGANIZER_ONE, m -> m.path("Subject").asString().contains(reference));
    String text = message.path("Text").asString();
    String internalId = registrationRow(reference).get("id");
    assertThat(internalId).isNotNull();
    assertThat(text)
        .doesNotContainIgnoringCase("exception")
        .doesNotContain("jdbc:")
        .doesNotContain("password")
        .doesNotContain(TestEnvironment.ORGANIZER_USERNAME)
        .doesNotContain(TestEnvironment.JSON_DIR.toString())
        .doesNotContain("si.konferenca")
        .doesNotContainPattern("(?im)^\\s*id\\s*:\\s*" + internalId + "\\s*$");
    JsonNode attachment = message.path("Attachments").get(0);
    JsonNode attached =
        JSON.readTree(
            MAILPIT.part(message.path("ID").asString(), attachment.path("PartID").asString()));
    assertThat(attached.propertyNames())
        .containsExactlyInAnyOrder(
            "schemaVersion",
            "reference",
            "submittedAt",
            "type",
            "participant",
            "options",
            "consents");
  }
}
