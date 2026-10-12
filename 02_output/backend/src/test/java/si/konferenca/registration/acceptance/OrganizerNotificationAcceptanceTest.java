package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.Mailpit.Message;
import si.konferenca.registration.acceptance.support.TestStack;

/** US-007 Organizer notification with the JSON copy attached. */
class OrganizerNotificationAcceptanceTest extends AcceptanceTestBase {

  private static final List<String> ORGANIZERS =
      List.of(TestStack.ORGANIZER_EMAIL_1, TestStack.ORGANIZER_EMAIL_2);

  @DynamicPropertySource
  static void settings(DynamicPropertyRegistry registry) {
    TestStack.register(registry);
  }

  @Test
  @DisplayName("AC-007-01 each organizer address receives the external registration data")
  void ac_007_01_eachOrganizerReceivesExternalRegistration() throws Exception {
    register(validExternal());

    mailpit.awaitAtLeast(3);
    for (String organizer : ORGANIZERS) {
      List<Message> messages = mailpit.to(organizer);
      assertThat(messages).as("notifications for %s", organizer).hasSize(1);
      assertThat(messages.getFirst().text())
          .contains("Ana")
          .contains("Novak")
          .contains("ana.novak@example.org")
          .contains("Institut Example")
          .contains("Workshop: Software testing")
          .contains("Lunch");
    }
  }

  @Test
  @DisplayName("AC-007-01 each organizer address receives the student registration data")
  void ac_007_01_eachOrganizerReceivesStudentRegistration() throws Exception {
    register(validStudent());

    mailpit.awaitAtLeast(3);
    for (String organizer : ORGANIZERS) {
      List<Message> messages = mailpit.to(organizer);
      assertThat(messages).as("notifications for %s", organizer).hasSize(1);
      assertThat(messages.getFirst().text())
          .contains("Luka")
          .contains("Kranjc")
          .contains("luka.kranjc@student.example.org")
          .contains("Univerza Example")
          .contains("Computer Science")
          .contains("63200001")
          .contains("Opening reception");
    }
  }

  @Test
  @DisplayName("AC-007-02 the notification has a JSON attachment equal to the stored JSON copy")
  void ac_007_02_attachmentEqualsJsonCopy() throws Exception {
    String id = register(validExternal());
    byte[] copy = Files.readAllBytes(jsonCopy(id));

    mailpit.awaitAtLeast(3);
    for (String organizer : ORGANIZERS) {
      Message message = mailpit.to(organizer).getFirst();
      List<Map<String, Object>> jsonAttachments =
          message.attachments().stream()
              .filter(a -> String.valueOf(a.get("ContentType")).startsWith("application/json"))
              .toList();
      assertThat(jsonAttachments).as("JSON attachments for %s", organizer).hasSize(1);
      Map<String, Object> attachment = jsonAttachments.getFirst();
      assertThat(String.valueOf(attachment.get("FileName"))).endsWith(".json");
      byte[] attached = mailpit.part(message.id(), String.valueOf(attachment.get("PartID")));
      assertThat(attached).isEqualTo(copy);
    }
  }
}
