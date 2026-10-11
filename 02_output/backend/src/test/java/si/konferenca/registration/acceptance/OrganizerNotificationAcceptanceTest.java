package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static si.konferenca.registration.acceptance.support.Payloads.external;
import static si.konferenca.registration.acceptance.support.Payloads.student;

import com.jayway.jsonpath.DocumentContext;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.TestEnvironment;

/** US-007 Organizer notification. */
class OrganizerNotificationAcceptanceTest extends AcceptanceTestBase {

  @Test
  @DisplayName("AC-007-01: every organizer address receives the registration data")
  void ac_007_01_eachOrganizerReceivesNotification() {
    Map<String, Object> submission = external();
    String lastName = (String) submission.get("lastName");

    registerAccepted(submission);

    for (String organizer :
        List.of(TestEnvironment.ORGANIZER_EMAIL_1, TestEnvironment.ORGANIZER_EMAIL_2)) {
      DocumentContext message =
          mailpit.awaitMessageTo(organizer, m -> m.<String>read("$.Text").contains(lastName));
      assertThat(message.<String>read("$.Text"))
          .contains((String) submission.get("firstName"))
          .contains((String) submission.get("email"))
          .contains((String) submission.get("organization"))
          .contains("Workshop: AI v praksi")
          .contains("Kosilo");
    }
  }

  @Test
  @DisplayName("AC-007-02: the notification carries the stored JSON copy as attachment")
  void ac_007_02_notificationHasJsonCopyAttached() throws IOException {
    Map<String, Object> submission = student();
    String lastName = (String) submission.get("lastName");

    UUID id = registerAccepted(submission);

    DocumentContext message =
        mailpit.awaitMessageTo(
            TestEnvironment.ORGANIZER_EMAIL_1, m -> m.<String>read("$.Text").contains(lastName));
    List<Map<String, Object>> attachments = message.read("$.Attachments");
    assertThat(attachments).hasSize(1);
    Map<String, Object> attachment = attachments.get(0);
    assertThat(attachment.get("FileName")).isEqualTo("registration-" + id + ".json");
    assertThat((String) attachment.get("ContentType")).startsWith("application/json");
    byte[] content = mailpit.part(message.read("$.ID"), String.valueOf(attachment.get("PartID")));
    assertThat(content).isEqualTo(Files.readAllBytes(jsonCopyFile(JSON_COPY_DIR, id)));
  }
}
